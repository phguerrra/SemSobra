# SemSobra

Aplicativo Android para acompanhar a produção de alimentos em restaurantes e buffets, registrar sobras e faltas e apoiar o planejamento das próximas refeições com base no histórico de consumo.

O projeto está em desenvolvimento. As regras de negócio e a persistência são executadas dentro do aplicativo, em Java e Kotlin, sem Spring Boot ou servidor separado. Os dados ficam salvos localmente com Room sobre SQLite.

## Funcionalidades

- **Preparos e cardápio:** cadastro, edição, listagem e exclusão de preparos, com nome, composição, unidade de medida e seleção dos dias da semana.
- **Produção do dia:** registro das quantidades produzidas e atualização da produção enquanto ela estiver em aberto.
- **Fechamento:** registro de clientes atendidos, sobras e preparos que acabaram antes do fim do atendimento, incluindo o horário.
- **Histórico:** consulta das produções e correção de dados de fechamentos anteriores.
- **Análises:** acompanhamento das sobras por unidade de medida e dos preparos com mais sobras ou faltas.
- **Previsão de demanda:** estimativa de clientes e quantidades para o próximo serviço, com indicação da qualidade e da origem do histórico utilizado.
- **Persistência local:** manutenção dos registros ao fechar e reabrir o aplicativo.

## Tecnologias

- Java e Kotlin.
- Jetpack Compose e Material 3 para a interface.
- ViewModel e Navigation Compose.
- Coroutines e Flow para operações em segundo plano e atualização de dados.
- Room sobre SQLite para armazenamento local.
- JUnit e AndroidX Test para testes unitários e instrumentados.
- Gradle Wrapper para compilação e gerenciamento das dependências.

As versões das dependências estão em [gradle/libs.versions.toml](gradle/libs.versions.toml).

## Organização do código

O código principal fica em `app/src/main/java/com/project/semsobra`:

```text
com.project.semsobra/
├── MainActivity.kt          # Entrada do aplicativo
├── data/
│   ├── local/room/          # Banco, entidades, DAOs e migrações
│   ├── mapper/              # Conversão do histórico para os cálculos
│   └── repository/          # Implementações da persistência local
├── domain/
│   ├── analytics/           # Cálculos de consumo, sobras e recomendações
│   ├── exception/           # Erros específicos das regras de negócio
│   ├── model/               # Modelos e política de precisão das quantidades
│   ├── previsao/            # Motor e modelos de previsão de demanda
│   ├── repository/          # Contratos de acesso aos dados
│   └── usecase/             # Casos de uso e validações
└── ui/
    ├── components/          # Componentes visuais reutilizáveis
    ├── model/               # Modelos e eventos da interface
    ├── navigation/          # Rotas do aplicativo
    ├── screens/             # Telas em Compose
    └── util/                # Formatação e leitura de campos
```

O ViewModel utiliza os casos de uso e os contratos do domínio. A `SemSobraViewModelFactory` monta as dependências e conecta os casos de uso às implementações locais.

Outras pastas importantes:

- `app/src/test`: testes unitários executados na JVM.
- `app/src/androidTest`: testes instrumentados, incluindo persistência e migrações.
- `app/schemas`: esquemas exportados pelo Room.
- `app/src/main/res`: recursos Android, como textos, temas e ícones.

## Como executar

### Requisitos

- Android Studio com suporte às versões do Android Gradle Plugin e do Gradle utilizadas pelo projeto.
- Android SDK API 37 para compilar.
- Emulador ou celular com Android 12 (API 31) ou superior.
- Acesso à internet na primeira sincronização para baixar ferramentas e dependências.

O projeto usa Gradle 9.5.0 e Android Gradle Plugin 9.3.2. O daemon do Gradle está configurado para Java 25 em [gradle/gradle-daemon-jvm.properties](gradle/gradle-daemon-jvm.properties); os alvos de compilação Java e Kotlin do app são 17. Utilize o Gradle Wrapper incluído no repositório.

### No Android Studio

1. Clone o repositório:

   ```bash
   git clone https://github.com/phguerrra/SemSobra.git
   ```

2. Abra a pasta raiz `SemSobra`, que contém `settings.gradle.kts`. Não abra somente `app` ou `app/src/main/java`.
3. Aguarde a sincronização do Gradle e instale os componentes do SDK solicitados.
4. Crie um emulador no Device Manager ou conecte um celular com depuração USB habilitada.
5. Selecione a configuração `app`, escolha o dispositivo e clique em **Run**.

O Android Studio configura o caminho do SDK em `local.properties`. Esse arquivo é específico de cada computador e não deve ser enviado ao Git.

Não é necessário iniciar uma API, configurar um servidor de banco de dados ou usar o Postman para utilizar o aplicativo.

### Gerar o APK de desenvolvimento

Na raiz do projeto, pelo PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

O APK é gerado em `app/build/outputs/apk/debug/app-debug.apk`.

Em Linux ou macOS, utilize `./gradlew` no lugar de `.\gradlew.bat` nos comandos desta documentação. Caso necessário, conceda permissão de execução com `chmod +x gradlew`.

## Fluxo de uso

1. Em **Preparo do Buffet**, cadastre os preparos e selecione seus dias no cardápio.
2. Em **Produção do Dia**, informe as quantidades produzidas e salve.
3. Em **Fechamento**, informe os clientes atendidos, as sobras e eventuais faltas.
4. Em **Análise**, consulte o resumo, os detalhes da previsão e o histórico.

A previsão depende dos registros anteriores. Sem histórico válido, o aplicativo informa que ainda não há dados suficientes. A estimativa geral em quilos utiliza os itens registrados em `kg`; os totais de sobras são separados por unidade, sem somar unidades diferentes.

## Regras e proteção dos dados

- Preparos utilizados em produções são inativados ao serem removidos, preservando os vínculos do histórico. Preparos sem uso podem ser excluídos.
- Preparos inativos não podem entrar em novas produções.
- Nomes de preparos são normalizados e verificados para evitar duplicidade no mesmo dia de cadastro.
- Quantidades produzidas devem ser finitas, positivas e não ultrapassar `10.000` na unidade informada.
- As quantidades persistidas são arredondadas para três casas decimais. Valores produzidos que arredondem para zero são rejeitados; sobra zero é permitida.
- A sobra não pode ser negativa nem maior que a quantidade produzida.
- O fechamento exige entre `1` e `10.000` clientes e horário no formato `HH:mm` quando um preparo acabou antes do fim.
- O fechamento valida os identificadores e exige todos os itens atuais da produção, sem repetições ou itens de outra produção.
- Salvamento e fechamento usam transações para evitar alterações parciais em caso de falha.
- O banco `semsobra.db` está na versão 7, com migrações das versões anteriores e testes de preservação de dados.

Os dados são armazenados no dispositivo, sem sincronização entre aparelhos. Fechar o app não apaga os registros; limpar os dados do aplicativo remove o banco local.

## Testes

Execute os testes unitários:

```powershell
.\gradlew.bat testDebugUnitTest
```

Com um emulador ou dispositivo de teste conectado, execute os testes instrumentados:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Para compilar o app e o APK de testes, sem executar testes no dispositivo:

```powershell
.\gradlew.bat assembleDebug assembleDebugAndroidTest
```

A cobertura inclui casos de uso de preparos e produção, validações numéricas, arredondamento, cálculos de análises e previsão, DAOs, integridade do fechamento e migrações do banco.

## Acompanhamento

As tarefas e os checklists do projeto são acompanhados no [quadro SemSobra no Trello](https://trello.com/b/j9lwKXnp/semsobra).

Para contribuir, mantenha a separação entre interface, domínio e persistência, adicione testes para as regras alteradas e organize as mudanças em commits pequenos por funcionalidade.
