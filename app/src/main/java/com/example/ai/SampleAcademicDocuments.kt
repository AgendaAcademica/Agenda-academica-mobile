package com.example.ai

object SampleAcademicDocuments {

    val SAMPLES = listOf(
        SampleDoc(
            id = "eng_soft",
            title = "Plano de Ensino — Engenharia de Software",
            fileName = "Plano_Ensino_Eng_Software_2026_2.pdf",
            disciplineName = "Engenharia de Software",
            disciplineCode = "ES204",
            professor = "Prof. João Silva",
            term = "2026/2",
            classroom = "Lab Info 04 - Bloco C",
            schedule = "Terça e Quinta, 14:00 - 16:00",
            colorHex = "#006E1C", // Verde Floresta Profissional
            content = """
                UNIVERSIDADE TECNOLÓGICA FEDERAL
                PLANO DE ENSINO - SEMESTRE 2026/2
                
                DISCIPLINA: Engenharia de Software (ES204)
                PROFESSOR: João Silva (joao.silva@universidade.edu.br)
                TURMA: ES-T01 | HORÁRIO: Terça e Quinta 14:00 - 16:00
                SALA: Lab Info 04 - Bloco C
                
                EMENTA:
                Processos de desenvolvimento de software, metodologias ágeis (Scrum/Kanban),
                engenharia de requisitos, modelagem UML, arquitetura de software e testes.
                
                CRONOGRAMA DE AVALIAÇÕES E ATIVIDADES:
                - 10/09/2026 14:00: Entrega da Atividade 1 - Documento de Requisitos de Software (SRS)
                - 24/09/2026 14:00: Prova 1 (P1) - Engenharia de Requisitos e Modelagem Ágil
                - 15/10/2026 14:00: Apresentação de Seminário - Padrões de Projeto Gang of Four
                - 30/10/2026 23:59: Entrega do Projeto - Módulo 1 (Arquitetura e Clean Architecture)
                - 20/11/2026 14:00: Prova 2 (P2) - Arquitetura de Software e Testes Automatizados
                - 05/12/2026 14:00: Entrega Final do Projeto Prático e Apresentação para Banca
            """.trimIndent()
        ),
        SampleDoc(
            id = "calculo",
            title = "Cronograma de Aulas e Provas — Cálculo Diferencial e Integral I",
            fileName = "Cronograma_Calculo_1_2026_2.pdf",
            disciplineName = "Cálculo",
            disciplineCode = "MAT101",
            professor = "Profa. Elena Ramos",
            term = "2026/2",
            classroom = "Auditório Central 102",
            schedule = "Segunda e Quarta, 08:00 - 10:00",
            colorHex = "#0061A4", // Azul Acadêmico
            content = """
                DEPARTAMENTO DE MATEMÁTICA E ESTATÍSTICA
                CRONOGRAMA ACADÊMICO - 2026/2
                
                DISCIPLINA: Cálculo Diferencial e Integral I (MAT101)
                PROFESSORA: Dra. Elena Ramos
                HORÁRIOS: Segunda e Quarta-feira, 08:00 - 10:00
                SALA: Auditório Central 102
                
                PROGRAMAÇÃO DE AULAS E PROVAS:
                - 01/09/2026 08:00: Aula - Revisão de Funções, Limites e Continuidade
                - 08/09/2026 23:59: Lista de Exercícios 1 - Limites Fundamentais e Teorema do Confronto
                - 22/09/2026 08:00: Prova 1 (P1) - Limites, Continuidade e Derivadas Básicas
                - 14/10/2026 23:59: Lista de Exercícios 2 - Regras da Cadeia e Taxas Relacionadas
                - 04/11/2026 08:00: Prova 2 (P2) - Aplicações de Derivadas e Introdução a Integrais
                - 25/11/2026 08:00: Prova Final (P3) - Integrais Definidas, Indefinidas e Áreas
                - 09/12/2026 08:00: Exame Especial / Prova Substitutiva
            """.trimIndent()
        ),
        SampleDoc(
            id = "banco_dados",
            title = "Plano de Disciplina — Banco de Dados",
            fileName = "Plano_Ensino_Banco_de_Dados_2026.pdf",
            disciplineName = "Banco de Dados",
            disciplineCode = "BD301",
            professor = "Prof. Carlos Mendes",
            term = "2026/2",
            classroom = "Lab de Redes e Dados 02",
            schedule = "Segunda e Quarta, 18:30 - 20:30",
            colorHex = "#8B5000", // Âmbar Acadêmico
            content = """
                DEPARTAMENTO DE COMPUTAÇÃO APLICADA
                PLANO DE DISCIPLINA - 2026/2
                
                DISCIPLINA: Banco de Dados Relacional e NoSQL (BD301)
                PROFESSOR: Me. Carlos Mendes
                HORÁRIO: Segunda e Quarta 18:30 - 20:30
                SALA: Lab de Redes e Dados 02
                
                CRONOGRAMA DE ATIVIDADES E AVALIAÇÕES:
                - 01/09/2026 18:30: Aula - Normalização de Dados (1FN, 2FN, 3FN e BCNF)
                - 12/09/2026 23:59: Entrega do Trabalho Prático 1 - Modelagem Conceitual e Lógica (DER)
                - 29/09/2026 18:30: Prova Prática 1 - Consultas Complexas em SQL, Joins e Subqueries
                - 19/10/2026 23:59: Entrega do Trabalho Prático 2 - Índices, Triggers e Procedures
                - 16/11/2026 18:30: Prova 2 - Transações ACID, Concorrência e Bancos NoSQL (MongoDB)
                - 02/12/2026 18:30: Apresentação do Projeto Integrador de Banco de Dados
            """.trimIndent()
        ),
        SampleDoc(
            id = "estrutura_dados",
            title = "Programa de Curso — Estrutura de Dados",
            fileName = "Programa_Estrutura_Dados_2026_2.pdf",
            disciplineName = "Estrutura de Dados",
            disciplineCode = "ED202",
            professor = "Profa. Fernanda Costa",
            term = "2026/2",
            classroom = "Sala 204 - Bloco A",
            schedule = "Sexta-feira, 08:00 - 12:00",
            colorHex = "#6750A4", // Roxo Profissional
            content = """
                DEPARTAMENTO DE CIÊNCIA DA COMPUTAÇÃO
                PROGRAMA DE CURSO - SEMESTRE 2026/2
                
                DISCIPLINA: Estrutura de Dados e Algoritmos (ED202)
                PROFESSORA: Dra. Fernanda Costa
                HORÁRIO: Sexta-feira 08:00 - 12:00
                SALA: Sala 204 - Bloco A
                
                AVALIAÇÕES E MARCOS DO SEMESTRE:
                - 04/09/2026 08:00: Aula - Listas Encadeadas, Pilhas e Filas
                - 18/09/2026 23:59: Exercício Prático 1 - Implementação de Árvores Binárias de Busca (BST)
                - 02/10/2026 08:00: Prova 1 (P1) - Estruturas Lineares e Árvores AVL/Rubro-Negra
                - 23/10/2026 23:59: Projeto 1 - Sistema de Indexação de Arquivos com Hash Tables
                - 13/11/2026 08:00: Prova 2 (P2) - Algoritmos de Grafos (Dijkstra, BFS, DFS) e Heap
                - 04/12/2026 08:00: Seminário de Encerramento - Estruturas de Dados Avançadas
            """.trimIndent()
        )
    )
}

data class SampleDoc(
    val id: String,
    val title: String,
    val fileName: String,
    val disciplineName: String,
    val disciplineCode: String,
    val professor: String,
    val term: String,
    val classroom: String,
    val schedule: String,
    val colorHex: String,
    val content: String
)
