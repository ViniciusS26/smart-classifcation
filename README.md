# Classificação distribuída de mensagens com RabbitMQ

MVP da Atividade 5: um gerador publica mensagens em um broker RabbitMQ, que as distribui para dois consumidores Java independentes. Cada consumidor classifica um tipo de mensagem usando um modelo local.

## Arquitetura

![Diagrama da arquitetura dos serviços definidos no Docker Compose](docs/arquitetura.svg)

O `docker-compose.yml` inicia quatro serviços na rede Docker `app-network`:

1. **rabbitmq** — broker RabbitMQ 3.13 com exchange `messages.topic`, filas e dead-letter queues (DLQs). O volume `rabbitmq_data` persiste os dados do broker; as portas `5672` e `15672` ficam disponíveis para AMQP e para a interface de administração.
2. **generator** — aplicação Java que produz mensagens de avaliação (`review.product`) e solicitações de suporte (`support.request`) e as publica no exchange.
3. **sentiment-consumer** — consome `reviews.queue` e classifica o sentimento das avaliações.
4. **support-consumer** — consome `support.queue` e classifica o assunto das solicitações de suporte.

O exchange do tipo `topic` roteia cada chave para a fila correspondente. Mensagens malformadas ou com `[FORCE_ERROR]` são rejeitadas sem requeue e encaminhadas pelo exchange `messages.dlx` às filas `reviews.dlq` ou `support.dlq`. Os consumidores usam ACK manual e prefetch de uma mensagem.

O gerador publica uma mensagem a cada 150 ms. Cada consumidor aguarda 1,2 s após classificar uma mensagem, por padrão; por isso, as filas principais podem crescer durante a demonstração.

## IAs e classificação

O projeto não chama APIs de IA nem serviços externos. Os dois consumidores usam classificadores locais **Multinomial Naive Bayes**, disponibilizados pela biblioteca Java **SMILE**. Na inicialização, cada modelo é treinado com exemplos de texto rotulados embutidos no próprio código. O texto é normalizado, tokenizado e convertido em frequências de palavras para a classificação.

- **Sentimento (`sentiment-consumer`)** — atribui às avaliações uma das classes `positivo`, `neutro` ou `negativo`.
- **Categoria de suporte (`support-consumer`)** — classifica solicitações como `acesso`, `desempenho` ou `conexão`.

Esses modelos demonstrativos são pequenos e treinados apenas com os exemplos do projeto; não são modelos generativos nem serviços remotos.

## Requisitos

- Docker com Docker Compose v2.
- Para executar os testes Maven localmente: JDK 21 e Maven 3.9 ou superior.

## Instruções de execução

Na raiz do projeto, inicie os serviços:

```powershell
docker compose up --build
```

O gerador e os consumidores mostram as mensagens e classificações nos logs. Para acompanhar os logs em outro terminal:

```powershell
docker compose logs -f
```

A interface de administração do RabbitMQ fica em <http://localhost:15672> (usuário `app`, senha `app` por padrão). Nela, é possível observar `reviews.queue`, `support.queue` e as DLQs `reviews.dlq` e `support.dlq`.

Para simular falhas e demonstrar o roteamento para as DLQs, configure `SIMULATE_FAILURE_EVERY` antes de iniciar o Compose. O exemplo abaixo adiciona uma falha a cada dez mensagens:

```powershell
$env:SIMULATE_FAILURE_EVERY = "10"
docker compose up --build
```

Também é possível ajustar a espera de processamento dos consumidores, que por padrão é de 1200 ms:

```powershell
$env:PROCESSING_DELAY_MS = "300"
docker compose up --build
```

Para personalizar as credenciais do broker, defina `RABBITMQ_USER` e `RABBITMQ_PASSWORD` antes de iniciar os serviços. Os valores padrão são `app` e `app`.

Para encerrar os serviços:

```powershell
docker compose down
```

Para remover também o volume persistente do RabbitMQ e zerar as filas:

```powershell
docker compose down --volumes
```

### Prints da execução

As imagens abaixo mostram uma execução do projeto. Os números de mensagens e as taxas variam conforme o tempo de execução.

#### Vídeo demonstração

[![Prévia do vídeo de demonstração](https://img.youtube.com/vi/2jZt-6Rqlkg/hqdefault.jpg)](https://youtu.be/2jZt-6Rqlkg)

#### Docker Desktop

O Docker Desktop mostra os quatro containers em execução: o broker RabbitMQ, o gerador e os dois consumidores.

![Containers do projeto em execução no Docker Desktop](docs/tela_gerador.png)

O log do gerador registra cada mensagem publicada, seu identificador e a chave de roteamento (`review.product` ou `support.request`).

![Log do gerador publicando mensagens](docs/execucao_container_sentiment_suport.png)

O consumidor de suporte registra as solicitações recebidas, o conteúdo e a categoria identificada, como `acesso`, `desempenho` ou `conexão`.

![Log do consumidor de suporte](docs/execucao_container_consumer_suport.png)

O consumidor de sentimento registra as avaliações recebidas e a classificação atribuída (`positivo`, `neutro` ou `negativo`).

![Log do consumidor de sentimento](docs/tela_home_container.png)

#### Interface de administração do RabbitMQ

A tela Overview resume o estado do broker, incluindo conexões, canais, exchanges, filas e consumidores, além das taxas de publicação e entrega. O gráfico de mensagens enfileiradas representa o estado no momento da captura.

![Visão geral do RabbitMQ e das filas](docs/tela_conections.png)

A lista Connections mostra as três conexões ativas com o broker: `message-generator`, `sentiment-consumer` e `support-consumer`.

![Conexões ativas no RabbitMQ](docs/home_rabbitmq.png)

O detalhe da conexão `message-generator` exibe seu estado e as taxas de tráfego entre o gerador e o broker.

![Detalhes da conexão do gerador no RabbitMQ](docs/execucao_gerador_mensagen.png)

O detalhe da conexão `support-consumer` mostra o consumidor conectado e informações do canal, como prefetch e mensagens não confirmadas.

![Detalhes da conexão do consumidor de suporte no RabbitMQ](docs/tela_consumidor_sentiment.png)

O detalhe da conexão `sentiment-consumer` apresenta as mesmas métricas para o consumidor de sentimento.

![Detalhes da conexão do consumidor de sentimento no RabbitMQ](docs/tela_consumidor_suport.png)

### Compilar e testar sem Docker

```powershell
mvn test
```

## Estrutura do projeto

```text
common/              Contrato da mensagem, topologia RabbitMQ e classificador compartilhado
generator/           Gerador e publicador de mensagens
sentiment-consumer/  Consumidor e modelo local de sentimento
support-consumer/    Consumidor e modelo local de suporte
Dockerfile           Build Java reutilizado pelos três serviços
docker-compose.yml   Broker e três aplicações na rede Docker
docs/                Diagrama da arquitetura e prints da execução
```
