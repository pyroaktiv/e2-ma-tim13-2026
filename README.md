# Slagalica - Mobilna Aplikacija (Tim 13)

Ovaj projekat predstavlja implementaciju kviza **Slagalica** u okviru predmeta **Mobilne aplikacije** (2025/26). Aplikacija je razvijena u jeziku **Kotlin** primenom **troslojne arhitekture** (prezentacioni deo, poslovna logika i upravljanje podacima).

## 📌 Status projekta - Kontrolna tačka 1 (KT1)
U okviru KT1 fokus je bio na razvoju **grafičkog korisničkog interfejsa (GUI)**. Implementirane su sledeće celine:
* **Registracija i logovanje**: Forme za prijavu, registraciju (email, korisničko ime, region, lozinka) i resetovanje lozinke.
* **Igra "Moj broj"**: Interfejs za stopiranje brojeva (traženi i 6 ponuđenih), unos matematičkog izraza i potvrdu rešenja.
* **Igra "Korak po korak"**: Interfejs sa progresivnim otkrivanjem 7 nivoa tragova i poljem za unos rešenja.
* **Igra "Ko zna zna"**: Interfejs za brzi kviz opšte informisanosti sa tajmerom po pitanju i vizuelnim povratnim informacijama o tačnosti.
* **Igra "Spojnice"**: Intefejs za dve runde spajanja pojmova sa specifičnom logikom "popravke" gde drugi igrač dobija priliku da poveže preostale parove.
* **Igra "Skočko"**: Interfejs za pogađanje kombinacije znakova (6 pokušaja) sa vizuelnim indikatorima pogođenih mesta i simbola.
* **Igra "Asocijacije"**: Klasičan interfejs sa četiri kolone (A, B, C, D), poljima za rešenja kolona i poljem za konačno rešenje asocijacije.
* **Korisnički profil**: Prikaz podataka o igraču sa implementiranom funkcionalnošću odjave (Logout).
* **Notifikacije**: Interfejs za prikaz i upravljanje obaveštenjima unutar aplikacije.
* **Demo režim**: "Nastavi kao gost" opcija koja omogućava direktan pristup demonstraciji igara.

## 🚀 Kako pokrenuti aplikaciju

Pratite ove korake kako biste pokrenuli projekat u lokalnom razvojnom okruženju:

1.  **Kloniranje repozitorijuma**:
    ```bash
    git clone [https://github.com/](https://github.com/)[korisničko_ime]/[ime_projekta].git
    ```
    *Napomena: Primarna i stabilna grana projekta je **master**.*

2.  **Otvaranje u Android Studio**:
    * Pokrenite **Android Studio** (preporučena verzija Ladybug ili novija).
    * Idite na `File > Open` i odaberite root folder projekta.

3.  **Sinhronizacija i SDK**:
    * Sačekajte da se završi **Gradle sync**.
    * Projekat zahteva **JDK 11** (podesiti u `Settings > Build, Execution, Deployment > Build Tools > Gradle`).

4.  **Pokretanje**:
    * Kreirajte ili pokrenite emulator sa **Minimalnim SDK: API 30 (Android 11.0)**.
    * Kliknite na dugme **Run** (`Shift + F10`) u gornjem meniju.

## 🛠️ Tehničke karakteristike
* **Jezik**: Kotlin
* **Minimalni SDK**: API 30 (Android 11.0)
* **Arhitektura**: MVVM / Troslojna arhitektura
* **UI Komponente**:
    * **ViewBinding**: Za bezbedan pristup UI elementima.
    * **Jetpack Navigation**: Za upravljanje prelazima između fragmenata.
    * **ConstraintLayout & LinearLayout**: Za responzivan dizajn.
* **Resursi**: Implementirana podrška za više jezika (**srpski** kao primarni i **engleski**) kroz `strings.xml`.

## 🗺️ Navigacija i struktura
Navigacija je centralizovana kroz **Navigation Graph** fajlove u `res/navigation` folderu:
1. **`auth_navigation.xml`**: Upravlja tokom prijave i registracije unutar `AuthActivity`.
2. **`game_navigation.xml`**: Upravlja tokom igranja partija i prebacivanjem između mini-igara unutar `GameActivity`.

## 🎮 Uputstvo za dodavanje novih igara
Kako bi se održala konzistentnost i čistoća koda, nove igre (Student 2 i Student 3) treba dodavati prateći ove korake:

1. **Kreiranje Layout-a**:
    * XML fajl smestiti u `res/layout` sa prefiksom `fragment_` (npr. `fragment_spojnice.xml`).
    * Obavezno uključiti zajednički header na vrh ekrana:
      ```xml
      <include android:id="@+id/gameHeader" layout="@layout/layout_game_header" />
      ```

2. **Kreiranje Fragmenta**:
    * Klasa mora nasleđivati `BaseFragment` radi unificiranog `ViewBinding`-a i `showError` mehanizma.
    * Primer: `class SpojniceFragment : BaseFragment<FragmentSpojniceBinding>(FragmentSpojniceBinding::inflate)`.

3. **Registracija u Navigaciji**:
    * Dodati fragment u `game_navigation.xml`.
    * Povezati ga akcijom iz `DemoGameListFragment` kako bi asistent mogao da ga testira u KT1.

4. **Navigacioni poziv**:
    * Prelazak na igru vršiti preko: `findNavController().navigate(R.id.action_list_to_novaIgra)`.

---
**Članovi tima (Tim 13):**
* Student 1: Registracija, logovanje, Korak po korak, Moj broj, Igranje partija, Čet, Izazov.
* Student 2: Prikaz profila, Ko zna zna, Spojnice, Prikaz regiona, Napredovanje kroz lige, Prijatelji.
* Student 3: Notifikacije, Asocijacije, Skočko, Rang lista, Turnir, Dnevne misije.
