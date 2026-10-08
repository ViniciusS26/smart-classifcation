# Classificação distribuída de mensagens com RabbitMQ

MVP da Atividade 5: quatro contêineres (gerador, RabbitMQ e dois consumidores), com três aplicações Java independentes e um módulo compartilhado.

## Arquitetura

```text
generator --review.product----> messages.topic --> reviews.queue --> sentiment-consumer
          --support.request--> messages.topic --> support.queue --> support-consumer
                                                        |                 |
                                                        +--> messages.dlx <--+
                                                              |          |
                                                        reviews.dlq  support.dlq
```

- O gerador publica avaliações e solicitações de suporte em intervalos de 150 ms (mais de 5 mensagens/s).
- O exchange `topic` encaminha cada tipo de mensagem à fila correspondente.
- O consumidor de avaliações usa um classificador Naive Bayes local para identificar sentimento positivo, neutro ou negativo.
- O consumidor de suporte tem seu próprio classificador Naive Bayes local para classificar problemas de acesso, desempenho ou conexão.
- Os modelos são treinados na inicialização com exemplos de treinamento embutidos no respectivo consumidor. Não há chamada a serviços externos de IA.
- Cada consumidor limita o prefetch a uma mensagem e aguarda 1,2 s antes de confirmar o processamento; os ACKs são manuais e as filas podem crescer durante a demonstração.
- Mensagens malformadas ou contendo `[FORCE_ERROR]` são rejeitadas sem requeue e encaminhadas à DLQ específica do consumidor.

## Requisitos

- Docker com Docker Compose v2.
- Para executar os testes Maven localmente: JDK 21 e Maven 3.9+.

## Executar

Na raiz do projeto:

```powershell
docker compose up --build
```

O gerador e os consumidores imprimem as mensagens e classificações nos logs. A interface de administração do RabbitMQ fica em <http://localhost:15672> (usuário `app`, senha `app` por padrão). As filas de mensagens inválidas são `reviews.dlq` e `support.dlq`.

Para gerar mensagens de erro e demonstrar as DLQs, configure `SIMULATE_FAILURE_EVERY` antes de iniciar o Compose. Por exemplo, no PowerShell:

```powershell
$env:SIMULATE_FAILURE_EVERY = "10"
docker compose up --build
```

Para acompanhar o crescimento das filas, observe `reviews.queue` e `support.queue` na interface de administração enquanto os consumidores processam mais lentamente do que o gerador publica.

Para encerrar:

```powershell
docker compose down
```

Para remover também o volume persistente do RabbitMQ e zerar as filas:

```powershell
docker compose down --volumes
```

## Compilar e testar sem Docker

```powershell
mvn test
```

O projeto está dividido em:

```text
common/              Contrato da mensagem, topologia RabbitMQ e base dos consumidores
generator/           Gerador e publicador de mensagens
sentiment-consumer/  Consumidor e modelo local de sentimento
support-consumer/    Consumidor e modelo local de suporte
Dockerfile           Build Java reutilizado pelos três serviços
docker-compose.yml   Broker e três serviços na mesma rede Docker
```


