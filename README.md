# job4j_students
This project represents synchronous message streaming pattern. This pattern is not effective for kafka in general, but can be useful in special cases.

#### Description
Проект демонстрирует реализацию синхронного паттерна обработки сообщений кафки. Данный паттерн не является эффективным для такого брокера сообщений,
как какфка, которая нацелена на асинронную обработку сообщений, и можно сказать, является антипаттерном, но в особых случаях синхронная обработка данных
в кафке может быть полезной.

Для релизации синхронной обработки (+ без использования спринга) реализуем концепцию:
- используя хедер ("correlationId") при записи в топик запроса и в топик ответа;
- во избежание бесконечного цикла выставляем ограничение в виде таймаутеа и в случае неуспеха возвращаем ошибку пользователю.

#### Functionality:
- Kafka: synchronous message streaming pattern

#### Used technologies:
- Kafka 
- Slf4j for logging