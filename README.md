# Projeto Final da Disciplina de Sistemas Distribuidos - UFN

## Introdução

Jogo desenvolvido em Java baseado no jogo existente GARTIC

## Sobre o Jogo

É um jogo de adivinhação de desenhos, uma palavra é sorteada para o jogador do turno escolhido e este tem que desenhar a palavra, enquanto os outros participantes tentam acertar. Conforme os acertos é criada uma pontuação.

O mínimo de jogadores é 2
O jogo de TURNOS
Cada turno é sorteada uma palavra ao jogador da fila e este desenha enquanto os outros adivinham

## Tecnologias Utilizadas

- Comunicacao por Socket TCP

- BufferedReader, InputStreamReader e FileInputStream para a leitura do arquivo das palavras

- ArrayList<> para Clientes, Palavras, Pixels, AcertosDeRespostas

- Comandos enviados em formato de comando:argumento -> Comando:ConteudoRecebimento

- Processamento de comandos em Thread para cada cliente

- Estruturas de Condição e Repetição

- Componentes da Interface Gráfica do Java

## Demonstração de telas
![Tela de Inicio](/imagenstela/telainicio.png)
![Tela do Jogo](/imagenstela/telainicio.png)

## Autores
[Douglas Kihara](https://github.com/Kenjix/)
[Gabriel Castagna](https://github.com/castagnagh)