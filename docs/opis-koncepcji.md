# AgreeOnEat — koncepcja produktu

**Status:** robocza koncepcja  
**Nazwa robocza:** AgreeOnEat  
**Opis skrócony:** „Tinder do obiadów” — aplikacja pomagająca grupie zdecydować, co zjeść.

Dokument porządkuje dotychczasowe ustalenia. Pierwotny, chronologiczny zapis rozmowy pozostaje w pliku `koncepcja.md`.

## 1. Wizja produktu

AgreeOnEat pozwala członkom wspólnego domu wybierać posiłki przez swipe’owanie propozycji. System bierze pod uwagę preferencje całej grupy, wykrywa zgodność pomiędzy domownikami i pomaga podjąć decyzję zarówno dla jednego dnia, jak i całego tygodnia.

Docelowi użytkownicy i szczegółowy opis problemów, które aplikacja ma rozwiązywać, pozostają do doprecyzowania.

## 2. Użytkownicy i wirtualny dom

- Użytkownik może utworzyć wirtualny dom/pokój.
- Do pokoju można dodawać i usuwać domowników.
- Członkowie pokoju wspólnie uczestniczą w sesjach wybierania posiłków.
- Swipe’owanie jest asynchroniczne — jedna osoba może głosować np. o 15:00, kolejna o 19:00, a następna o 24:00.
- Wybory każdego użytkownika są zapisywane w ramach wspólnej sesji pokoju.

## 3. Rejestracja i profil użytkownika

### 3.1. Rejestracja i logowanie

Podstawowe dane konta:

- imię;
- adres e-mail używany jako login;
- pozostałe wymagane dane — do ustalenia.

Alternatywnie użytkownik może zarejestrować się i logować przez zewnętrznego dostawcę tożsamości, np. konto Google.

### 3.2. Stałe preferencje profilu

Po rejestracji użytkownik uzupełnia:

- listę alergenów;
- listę składników, których nie lubi.

Obie listy można później zmienić w ustawieniach, ale są traktowane jako względnie stałe dane profilu.

#### Alergeny

**Alergeny stanowią bezwzględne wykluczenie, a nie element rankingu.**

- Danie zawierające alergen użytkownika nigdy nie może zostać mu pokazane.
- Jeżeli użytkownik uczestniczy w sesji grupowej, danie zawierające jego alergen jest wykluczane dla całej grupy.
- Takie danie nie może pojawić się w rankingu, swipe’owaniu, fazie eliminacji ani planie tygodniowym.

#### Nielubiane składniki

Obecność nielubianego składnika obniża pozycję dania dla danego użytkownika, ale nie usuwa go całkowicie z propozycji.

## 4. Dynamiczne preferencje sesji

Przed rozpoczęciem każdej sesji użytkownik otrzymuje formularz zawierający:

- rodzaj, bazę lub główny składnik dania, na który ma obecnie ochotę, np. makaron, ryż, rybę albo wołowinę;
- pułap cenowy;
- liczbę kilokalorii na porcję;
- maksymalny czas przygotowania.

System pamięta wartości z poprzedniej sesji i automatycznie uzupełnia nimi formularz. Użytkownik może rozpocząć kolejną sesję bez zmian albo zmodyfikować tylko wybrane pola. Dzięki temu powtarzalne potrzeby, np. limit kalorii związany z dietą, nie wymagają ciągłego ustawiania.

## 5. Tryby planowania

Aplikacja ma dwa podstawowe tryby:

1. **Tryb jednorazowy** — grupa wybiera jeden obiad na konkretny dzień.
2. **Tryb tygodniowy** — grupa wybiera wiele obiadów, a następnie układa z nich menu na cały tydzień.

## 6. Wspólny przebieg sesji

1. Tworzona jest sesja planowania dla wybranego pokoju i trybu.
2. Przy rozpoczęciu sesji system tworzy niezmienny snapshot obejmujący uczestników, ich alergeny i preferencje oraz przygotowaną pulę dań.
3. Dodanie lub usunięcie domownika albo zmiana jego profilu po rozpoczęciu sesji nie wpływa na jej skład, próg matchu ani propozycje. Zmiany obowiązują od kolejnej sesji.
4. System przygotowuje indywidualną kolejność wyświetlania wspólnej puli dla każdego uczestnika.
5. Uczestnicy swipe’ują propozycje w dogodnym dla siebie czasie.
6. Warunek matchu jest sprawdzany po każdym nowym głosie.

Głosowanie w sesji trwa maksymalnie 12 godzin. Po przekroczeniu tego czasu możliwość oddawania głosów zostaje zamknięta, a brakujące głosy nie blokują zakończenia sesji.

Każdy członek pokoju musi zakończyć udział w sesji na jeden z dwóch sposobów:

- przejrzeć przygotowane propozycje;
- wybrać opcję **„Pomiń / zdaję się na pozostałych”**.

Użycie opcji „Pomiń” oznacza wstrzymanie się od głosu. Użytkownik nie blokuje zakończenia sesji, ale nie oddaje automatycznie głosu „tak” ani „nie” na żadne danie.

Osoba, która przejrzała całą pulę przed pozostałymi, otrzymuje komunikat:

> To już wszystkie propozycje — poczekaj na pozostałych domowników.

## 7. Zasady powstawania matchu

Danie zostaje oznaczone jako match, gdy jednocześnie:

- zaakceptowały je co najmniej 2 osoby;
- zaakceptowało je co najmniej 50% wszystkich członków pokoju przypisanych do sesji.

Reguła:

`liczba głosów „tak” ≥ max(2, zaokrąglone w górę 50% członków pokoju)`

Gdy nowy głos tworzy match:

- aktualnie swipe’ująca osoba od razu widzi informację o matchu;
- może kontynuować ocenianie pozostałych dań.

Sposób informowania osób, które skończyły wcześniej, o matchu utworzonym później pozostaje do ustalenia.

## 8. Algorytm dobierania dań

Algorytm działa w dwóch etapach: najpierw tworzy wspólną pulę propozycji, a następnie ustala indywidualną kolejność dla każdego użytkownika.

### 8.1. Filtrowanie alergenów

Punktem wyjścia jest cała baza, przykładowo 1000 dań. Przed obliczeniem rankingu system usuwa wszystkie dania zawierające alergen zadeklarowany przez dowolnego uczestnika sesji.

### 8.2. Ranking grupowy i wspólna pula

1. Każde pozostałe danie otrzymuje wynik określający, jak dobrze odpowiada sumie preferencji wszystkich osób w pokoju.
2. Baza jest sortowana malejąco według wyniku.
3. Do swipe’owania trafia około 50 najlepiej dopasowanych dań.

Liczba 50 jest robocza. Docelowo wspólna pula może zawierać np. 40, 50 albo 60 dań.

Przykład preferencji:

- osoba 1 ma ochotę na makaron;
- osoba 2 ma ochotę na kurczaka;
- osoba 3 ma ochotę na ser.

Przykładowy ranking grupowy:

1. makaron z kurczakiem w sosie serowym — odpowiada wszystkim trzem preferencjom;
2. makaron z kurczakiem w sosie grzybowym — odpowiada dwóm preferencjom;
3. kurczak z ryżem w sosie teriyaki — odpowiada jednej preferencji.

### 8.3. Indywidualna kolejność

- Każdy użytkownik otrzymuje ten sam zestaw około 50 dań.
- Zestaw jest ponownie sortowany według indywidualnych preferencji użytkownika.
- Zmienia się kolejność wyświetlania kafelków, ale nie skład wspólnej puli.
- Osoba mająca ochotę na ser zobaczy najpierw dania z serem, a później pozostałe propozycje.

W ten sposób każde proponowane danie pozostaje odpowiednie dla grupy, ale użytkownicy zaczynają od propozycji najlepiej odpowiadających ich własnym preferencjom.

### 8.4. Dane wyliczane przez algorytm

Na podstawie składników przepisu system ma obliczać:

- makroskładniki dania;
- szacowany koszt potrzebnych zakupów.

### 8.5. Rozważane rozwiązania techniczne

- K-NN (k najbliższych sąsiadów) jako możliwy sposób wyszukiwania i sortowania dań podobnych do zestawu preferencji;
- prostszy ranking punktowy oparty na wagach preferencji.

Oba podejścia wymagają porównania przed wyborem rozwiązania.

## 9. Tryb jednorazowy

Celem sesji jest wyłonienie jednego obiadu na konkretny dzień.

### 9.1. Brak matchu i faza eliminacji

Faza eliminacji uruchamia się, gdy wszyscy uczestnicy przejrzeli całą wspólną pulę, ale nie powstał żaden match.

System:

1. informuje grupę o braku zgodności;
2. wybiera najwyżej ocenione dania z przejrzanej puli według wzoru `liczba osób × 2 + 1`;
3. rozpoczyna fazę eliminacji;
4. każdy uczestnik po kolei banuje 2 dania;
5. ostatnie pozostałe danie staje się ostatecznym wyborem grupy.

Przykład dla 3 osób:

- początkowa pula eliminacyjna zawiera 7 dań;
- każda osoba odrzuca 2 dania;
- łącznie odpada 6 dań;
- ostatnie danie zostaje wybrane do zjedzenia.

## 10. Tryb tygodniowy

- W jednej sesji może powstać wiele matchy.
- Pierwszy match nie kończy swipe’owania.
- Wszystkie dopasowane dania trafiają do wspólnej puli matchy.
- Po zakończeniu wybierania użytkownik przechodzi do widoku tygodnia.
- Kafelki dań można przeciągać i przypisywać do konkretnych dni, tworząc tygodniowe menu.

## 11. Dane o przepisach

Potrzebna jest dobra i legalna baza zawierająca co najmniej:

- przepisy;
- ustrukturyzowane składniki;
- zdjęcia;
- dane pozwalające obliczać makroskładniki;
- dane pozwalające szacować koszt zakupów;
- pozostałe informacje wymagane przez filtry i ranking, m.in. czas przygotowania i kaloryczność porcji.

Nie kopiujemy całej zawartości AniaGotuje.pl. Jeden z członków zespołu ma skonsultować ze znajomym prawnikiem możliwość i warunki wykorzystywania cudzych przepisów oraz baz danych.

## 12. Architektura techniczna

### 12.1. Backend

- architektura mikroserwisowa;
- Spring Boot;
- Java 25;
- Maven;
- Docker;
- wdrożenie w chmurze AWS — konkretne usługi pozostają do wyboru.

### 12.2. Aplikacja mobilna

- React Native;
- aplikacja instalowana na telefonie;
- początkowo platforma Android;
- dystrybucja przez Google Play.

## 13. Organizacja pracy i jakość

- GitHub jest głównym miejscem zarządzania projektem.
- Zadania i pomysły są zapisywane jako GitHub Issues.
- Zmiany trafiają do projektu przez Pull Requesty.
- Każdy Pull Request musi przejść pipeline CI zawierający co najmniej build i testy automatyczne.
- Scalenie Pull Requesta wymaga akceptacji co najmniej jednego reviewera.

## 14. Otwarte decyzje

### Ranking i pula propozycji

- Czy wspólna pula powinna zawierać 40, 50, 60 czy dynamiczną liczbę dań?
- Jakie wagi przypisać ochocie na składnik, cenie, kaloriom, czasowi przygotowania i nielubianym składnikom?
- Czy wynik grupowy powinien być równoważony tak, aby preferencje jednej osoby nie zostały zdominowane przez resztę grupy?

### Tryb jednorazowy

- Jak ustalać kolejność banowania i czy powinna zmieniać się pomiędzy sesjami?
- Czy po pierwszym matchu grupa nadal szuka kolejnych matchy, a następnie wybiera spośród nich zwycięzcę?

### Tryb tygodniowy

- Co zrobić, gdy liczba matchy jest mniejsza albo większa od liczby dni do zaplanowania?

### Powiadomienia

- Jak informować osoby, które skończyły wcześniej, o matchu utworzonym później: powiadomieniem push, informacją w aplikacji czy obiema metodami?

## 15. Edge case’y do obsłużenia

### Pokój i skład sesji

- Pokój ma tylko jednego członka, przez co nie może spełnić warunku minimum 2 głosów.
- Właściciel pokoju opuszcza go albo usuwa konto — potrzebna jest reguła przejęcia własności lub zamknięcia pokoju.
- Domownik zostaje dodany, usunięty albo zmienia alergeny podczas aktywnej sesji — aktywna sesja korzysta ze swojego snapshotu, a zmiana obowiązuje dopiero w kolejnej.
- Dwie osoby próbują jednocześnie rozpocząć sesję dla tego samego pokoju i terminu.
- Użytkownik należy do więcej niż jednego pokoju.

### Czas trwania i głosowanie

- Użytkownik nie zagłosował ani nie wybrał opcji „Pomiń” przed upływem 12 godzin.
- Głos dociera dokładnie w momencie zamknięcia sesji.
- Ten sam głos zostaje wysłany ponownie, np. po ponowieniu żądania przy słabym połączeniu.
- Użytkownik chce zmienić już oddany swipe.
- Trzeba ustalić, w którym momencie zbierane są dynamiczne preferencje wszystkich osób przed utworzeniem snapshotu i czy można je jeszcze zmienić dla aktywnej sesji.

### Ranking i dane przepisów

- Po odfiltrowaniu alergenów nie zostaje żadne danie albo zostaje ich mniej niż zakładana wielkość puli.
- Przepis nie ma ceny, kaloryczności, czasu przygotowania albo pełnej listy składników.
- Preferencje uczestników wzajemnie się wykluczają.
- Najwyżej oceniona pula zawiera wiele niemal identycznych dań.
- Użytkownicy otrzymują te same propozycje w kolejnych sesjach.
- Trzeba odróżnić swipe w lewo oznaczający „nie dzisiaj” od trwałego dodania składnika lub dania do nielubianych.

### Match i powiadomienia

- Dwa równoczesne głosy tworzą ten sam match — system nie może utworzyć duplikatu ani wysłać wielokrotnych powiadomień.
- Match powstaje po zakończeniu swipe’owania przez część domowników.
- W trybie jednorazowym powstaje więcej niż jeden match.
- Przepis zostaje zmieniony albo usunięty po utworzeniu matchu.
- Użytkownik ma wyłączone powiadomienia push lub jest offline.

### Faza eliminacji

- Sekwencyjne banowanie daje ostatniej osobie możliwość praktycznego wskazania zwycięzcy przez usunięcie 2 z ostatnich 3 dań.
- Uczestnik nie bierze udziału w eliminacji lub opuszcza ją w trakcie.
- Po filtrach dostępnych jest mniej niż `2 × liczba osób + 1` dań.
- Kilka osób chce zbanować to samo danie — należy określić, czy ban jest wspólny, czy każda osoba musi wskazać dwa różne, nadal dostępne dania.

### Plan tygodniowy

- Liczba matchy jest mniejsza albo większa od liczby dni do zaplanowania.
- W planie występują dni bez gotowania, jedzenie poza domem albo wykorzystywanie resztek.
- Dwie osoby równocześnie przypisują dania do tego samego dnia.
- Trzeba określić, kto może zatwierdzić i później zmieniać gotowy plan.
- Zmiana planu powoduje, że wcześniej wygenerowana lista zakupów staje się nieaktualna.

### Alergeny, makro i koszt

- Alergen jest ukryty w składniku złożonym, np. sosie, pesto albo mieszance przypraw.
- Źródło danych nie podaje pełnej informacji o alergenach.
- Wartości odżywcze zależą od konkretnego produktu, porcji oraz tego, czy składnik podano w stanie surowym, czy po przygotowaniu.
- Koszt zużytej części składnika różni się od kosztu zakupu całego opakowania.
- Ceny zależą od sklepu, lokalizacji, promocji i momentu wykonania wyliczenia.

## 16. Pomysły na później

- Automatyczna lista składników lub zakupów dla tygodniowego menu.
- Dalsze opcjonalne rozszerzenia będą dopisywane w tej sekcji.
