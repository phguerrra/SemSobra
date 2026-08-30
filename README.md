## SEM Sobra

**SEM Sobra** é um aplicativo mobile para ajudar restaurantes a prever a demanda, planejar o preparo de alimentos e reduzir desperdício.

A ideia central é simples: o restaurante informa ou importa suas vendas, pratos e fichas técnicas, e o sistema sugere quanto preparar para o próximo dia ou turno.

## Objetivo

Reduzir sobra de comida, evitar falta de produtos e melhorar o planejamento da cozinha usando histórico de vendas, padrões de consumo e previsões simples de demanda.

## Problema

Restaurantes precisam decidir diariamente quanto produzir antes de saber exatamente quantas pessoas vão consumir. Quando produzem demais, há desperdício. Quando produzem pouco, perdem vendas e prejudicam a experiência do cliente.

## Solução

O SEM Sobra propõe uma ferramenta prática para:

- registrar vendas por prato, dia e turno;
- cadastrar pratos e ingredientes;
- calcular a quantidade recomendada de preparo;
- acompanhar estoque;
- gerar previsões de demanda;
- apoiar decisões da cozinha e da gestão.

## Funcionalidades Planejadas

- Cadastro de restaurantes
- Cadastro de pratos
- Cadastro de ingredientes
- Ficha técnica dos pratos
- Registro de vendas diárias
- Registro de estoque
- Previsão de clientes e pedidos
- Sugestão de preparo por prato
- Sugestão de compra de ingredientes
- Histórico de previsões
- Relatórios de sobra e desperdício

## MVP

A primeira versão pode começar simples, sem login e com dados locais ou manuais.

Funcionalidades sugeridas para o MVP:

- cadastro de pratos;
- cadastro de ingredientes;
- ficha técnica básica;
- lançamento manual de vendas;
- previsão simples por média de vendas;
- tela com sugestão de preparo para o próximo dia.

## Arquitetura Recomendada

```text
App Android
   ↓
API Backend
   ↓
Banco de Dados
```

### Frontend Mobile

O aplicativo Android pode ser desenvolvido com:

- Kotlin
- Jetpack Compose
- Android Studio

Responsável por:

- telas do app;
- navegação;
- formulários;
- gráficos;
- exibição das previsões;
- comunicação com o backend.

### Backend

O backend pode ser desenvolvido com:

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- PostgreSQL
- Spring Security, quando houver login

Responsável por:

- regras de negócio;
- API REST;
- persistência dos dados;
- cálculo das previsões;
- autenticação futura;
- integração com serviços externos.

### Banco de Dados

Banco recomendado:

- PostgreSQL

Principais entidades planejadas:

- Restaurante
- Usuario
- Prato
- Ingrediente
- FichaTecnica
- Venda
- Estoque
- Previsao
- PreparoRecomendado

## Previsão de Demanda

No começo, a previsão pode ser feita com regras simples:

- média por dia da semana;
- média por turno;
- histórico dos últimos dias;
- ajuste manual do gerente;
- margem de segurança.

Exemplo:

```text
Se nas últimas sextas-feiras foram vendidos em média 80 pratos,
o sistema pode sugerir preparar entre 75 e 85 porções.
```

Em versões futuras, o projeto pode usar modelos de previsão mais avançados, como:

- Prophet
- LightGBM
- StatsForecast
- modelos próprios em Python

## Segurança

Mesmo no começo, o projeto deve considerar:

- uso de HTTPS;
- proteção dos dados do restaurante;
- ausência de chaves secretas no app mobile;
- validação dos dados no backend;
- logs sem informações sensíveis;
- backups seguros;
- separação dos dados por restaurante;
- política de privacidade;
- adequação à LGPD.

Quando houver login, o sistema deve incluir:

- autenticação;
- controle de acesso por perfil;
- recuperação de senha;
- tokens seguros;
- permissões por restaurante ou unidade.

## Possíveis Perfis de Usuário

- Dono do restaurante
- Gerente
- Cozinha
- Estoque
- Administrador do sistema

## Roadmap

### Versão 1

- Estrutura do app Android
- Cadastro de pratos
- Cadastro de ingredientes
- Registro manual de vendas
- Previsão simples
- Sugestão de preparo

### Versão 2

- Backend Spring Boot
- Banco PostgreSQL
- Login
- Sincronização em nuvem
- Relatórios básicos

### Versão 3

- Previsão avançada
- Integração com delivery ou PDV
- Controle de estoque
- Alertas de compra
- Dashboard gerencial

## Como Executar

As instruções abaixo devem ser atualizadas conforme o projeto for implementado.

### App Android

1. Abrir o projeto no Android Studio.
2. Sincronizar as dependências.
3. Executar em um emulador ou dispositivo Android.

### Backend

1. Abrir o projeto no IntelliJ IDEA.
2. Configurar o banco PostgreSQL.
3. Rodar a aplicação Spring Boot.
4. Acessar a API localmente.

## Status

Projeto em fase inicial de definição e prototipação.

## Nome

**SEM Sobra** comunica diretamente o benefício principal do produto: ajudar restaurantes a produzirem melhor, venderem melhor e desperdiçarem menos.
