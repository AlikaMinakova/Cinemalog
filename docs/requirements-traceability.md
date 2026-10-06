# Матрица трассировки требований

| Требование | Реализация | Unit-тест |
|---|---|---|
| ФТ-1.1 | `JournalService.add` | `givenValidMovie_whenAdd_thenRepositorySavesCreatedRecord` |
| ФТ-1.2 Название | `RecordValidator` | `givenEmptyTitle...`, `given201CharTitle...` |
| ФТ-1.2 Год | `RecordValidator` | `givenYearBefore1888...` |
| ФТ-1.2 Оценка | `RecordValidator` / `JournalService.setRating` | `givenRatingOutside1To10...`, `givenWatchedRecord_whenSetRating...` |
| ФТ-1.2 Комментарий | `RecordValidator` / `JournalService.setComment` | `givenCommentOver2000Chars...`, `givenCommentTooLong...` |
| ФТ-1.3 Сериал | `SeriesProgress` / `RecordValidator` | `givenValidSeriesProgress...`, `givenWatchedEpisodesAbove...` |
| ФТ-1.4 Обязательные поля | `ValidationException` | `givenInvalidInput_whenAdd_thenRepositoryIsNotTouched` |
| ФТ-1.5 сохранение | `JournalService.add` + UI refresh | add test |
| ФТ-1.6/1.7 редактирование | `MainFrame` + `JournalService.edit` | edit tests |
| ФТ-1.8/1.9 удаление | `MainFrame.deleteSelected` | delete service test |
| ФТ-1.10 дубликаты | `existsDuplicate` + confirmation in UI | duplicate tests |
| ФТ-2.* статусы | `JournalService.changeStatus` + UI menu | status tests |
| ФТ-3.* оценка/заметки | service + table + note panel | rating/comment tests |
| ФТ-4.* прогресс | `SeriesProgress` + `incrementEpisode` | progress tests |
| ФТ-5.* фильтры/сортировка | `JournalService.query` | query tests |
| ФТ-6.* поиск | `JournalService.query` + UI | search test |
| ФТ-7.* статистика | `JournalService.statistics` | statistics tests |
| ФТ-8.* backup | `BackupService` / `JsonBackupStore` | backup service tests |
