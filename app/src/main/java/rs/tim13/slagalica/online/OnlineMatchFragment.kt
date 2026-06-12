package rs.tim13.slagalica.online

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import org.json.JSONArray
import org.json.JSONObject
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.core.util.TokenManager
import rs.tim13.slagalica.databinding.FragmentOnlineMatchBinding
import kotlin.math.ceil

/**
 * Hosts a complete real-time partija (all six games) for two players. It is a
 * pure view of the server-authoritative state: every screen transition, timer
 * and score comes from [GameClient] WebSocket events; the fragment only sends
 * the local player's moves.
 */
class OnlineMatchFragment : BaseFragment<FragmentOnlineMatchBinding>(FragmentOnlineMatchBinding::inflate) {

    private companion object {
        const val PAGE_MATCHMAKING = 0
        const val PAGE_KOZNAZNA = 1
        const val PAGE_SPOJNICE = 2
        const val PAGE_RESULT = 3
        const val PAGE_SKOCKO = 4
        const val PAGE_KORAK = 5
        const val PAGE_MOJBROJ = 6
        const val PAGE_ASO = 7
        val SKOCKO_SYMBOLS = listOf("🐵", "🟦", "🔵", "❤️", "🔺", "⭐")
    }

    private val listener = GameClient.Listener { onEvent(it) }
    private val handler = Handler(Looper.getMainLooper())

    private var mySlot = 0
    private val names = arrayOf("Igrač 1", "Igrač 2")
    private var opponentName = "Protivnik"
    private var matchOver = false

    private var kzzAnswered = false

    private var spjMyTurn = false
    private var selectedLeft: Int? = null
    private val connectedLeft = BooleanArray(5)
    private val usedRight = BooleanArray(5)

    // Skočko
    private var skockoActive = false
    private val skockoCurrent = mutableListOf<Int>()
    private val skockoHistory = StringBuilder()

    // Korak po korak
    private var korakActive = false

    // Moj broj
    private val mbTokens = mutableListOf<String>()
    private var mbNumbers = IntArray(0)
    private var mbSubmitted = false

    // Asocijacije
    private var asoActive = false

    private var displayTimer: CountDownTimer? = null

    private lateinit var answerButtons: List<MaterialButton>
    private lateinit var leftButtons: List<MaterialButton>
    private lateinit var rightButtons: List<MaterialButton>
    private lateinit var symbolButtons: List<MaterialButton>
    private lateinit var mbNumButtons: List<MaterialButton>
    private lateinit var asoFieldButtons: List<List<MaterialButton>>
    private lateinit var asoSolveButtons: List<MaterialButton>
    private lateinit var asoColHeaders: List<android.widget.TextView>

    private var defaultTint: ColorStateList? = null
    private var defaultTextColor: ColorStateList? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        answerButtons = listOf(binding.btnAnsA, binding.btnAnsB, binding.btnAnsC, binding.btnAnsD)
        leftButtons = listOf(binding.btnL0, binding.btnL1, binding.btnL2, binding.btnL3, binding.btnL4)
        rightButtons = listOf(binding.btnR0, binding.btnR1, binding.btnR2, binding.btnR3, binding.btnR4)
        symbolButtons = listOf(binding.btnSym0, binding.btnSym1, binding.btnSym2, binding.btnSym3, binding.btnSym4, binding.btnSym5)
        mbNumButtons = listOf(binding.btnMbN0, binding.btnMbN1, binding.btnMbN2, binding.btnMbN3, binding.btnMbN4, binding.btnMbN5)
        asoFieldButtons = listOf(
            listOf(binding.btnAso00, binding.btnAso01, binding.btnAso02, binding.btnAso03),
            listOf(binding.btnAso10, binding.btnAso11, binding.btnAso12, binding.btnAso13),
            listOf(binding.btnAso20, binding.btnAso21, binding.btnAso22, binding.btnAso23),
            listOf(binding.btnAso30, binding.btnAso31, binding.btnAso32, binding.btnAso33),
        )
        asoSolveButtons = listOf(binding.btnAsoSolve0, binding.btnAsoSolve1, binding.btnAsoSolve2, binding.btnAsoSolve3)
        asoColHeaders = listOf(binding.tvAsoCol0, binding.tvAsoCol1, binding.tvAsoCol2, binding.tvAsoCol3)
        defaultTint = binding.btnAnsA.backgroundTintList
        defaultTextColor = binding.btnAnsA.textColors

        binding.flipper.displayedChild = PAGE_MATCHMAKING
        binding.tvMmStatus.text = "Povezivanje..."

        answerButtons.forEachIndexed { i, b -> b.setOnClickListener { onAnswerClicked(i) } }
        leftButtons.forEachIndexed { i, b -> b.setOnClickListener { onLeftClicked(i) } }
        rightButtons.forEachIndexed { i, b -> b.setOnClickListener { onRightClicked(i) } }
        symbolButtons.forEachIndexed { i, b -> b.setOnClickListener { onSkockoSymbol(i) } }
        binding.btnSkockoBack.setOnClickListener { if (skockoCurrent.isNotEmpty()) { skockoCurrent.removeAt(skockoCurrent.size - 1); renderSkockoCurrent() } }
        binding.btnSkockoSubmit.setOnClickListener { onSkockoSubmit() }
        binding.btnKorakSubmit.setOnClickListener { onKorakSubmit() }
        setupMojBroj()
        setupAsocijacije()

        binding.btnMmCancel.setOnClickListener { GameClient.sendCancelMatch(); leaveToMenu() }
        binding.btnResultBack.setOnClickListener { leaveToMenu() }

        GameClient.addListener(listener)
        if (GameClient.isConnected) {
            binding.tvMmStatus.text = "Tražim protivnika..."
            GameClient.sendQuickMatch()
        } else {
            GameClient.connect(requireContext())
        }
    }

    // ---- event routing ---------------------------------------------------

    private fun onEvent(e: JSONObject) {
        when (e.optString("type")) {
            "_open" -> { binding.tvMmStatus.text = "Tražim protivnika..."; GameClient.sendQuickMatch() }
            "queued" -> binding.tvMmStatus.text = "Tražim protivnika..."
            "match_found" -> onMatchFound(e)
            "game_intro" -> { /* per-game round events drive the UI */ }
            "kzz_question" -> onKzzQuestion(e)
            "kzz_opponent_answered" -> binding.tvKzzInfo.text = "Protivnik je odgovorio..."
            "kzz_result" -> onKzzResult(e)
            "spj_round" -> onSpjRound(e)
            "spj_connect" -> onSpjConnect(e)
            "spj_turn" -> onSpjTurn(e)
            "spj_round_end" -> binding.tvTurnInfo.text = "Kraj runde"
            "skocko_round" -> onSkockoRound(e)
            "skocko_feedback" -> onSkockoFeedback(e)
            "skocko_round_end" -> onSkockoRoundEnd(e)
            "korak_round" -> onKorakRound(e)
            "korak_clue" -> onKorakClue(e)
            "korak_wrong" -> { if (e.optInt("by") == mySlot) binding.tvKorakTurn.text = "Netačno, pokušaj ponovo" }
            "korak_correct" -> onKorakCorrect(e)
            "korak_steal" -> onKorakSteal(e)
            "korak_round_end" -> onKorakRoundEnd(e)
            "mojbroj_round" -> onMojBrojRound(e)
            "mojbroj_opponent_submitted" -> binding.tvMbTurn.text = "Protivnik je predao rešenje..."
            "mojbroj_result" -> onMojBrojResult(e)
            "aso_round" -> onAsoRound(e)
            "aso_field" -> onAsoField(e)
            "aso_solved" -> onAsoSolved(e)
            "aso_wrong" -> toast("Netačno")
            "aso_turn" -> onAsoTurn(e)
            "aso_round_end" -> onAsoRoundEnd(e)
            "game_complete" -> onGameComplete(e)
            "match_over" -> onMatchOver(e)
            "opponent_left" -> toast("Protivnik je napustio partiju")
            "error" -> toast(e.optString("message", "Greška"))
            "_failure", "_closed" -> if (!matchOver) toast("Veza je prekinuta")
        }
    }

    private fun onMatchFound(e: JSONObject) {
        mySlot = e.optInt("you", 0)
        opponentName = e.optString("opponent", "Protivnik")
        val myName = TokenManager(requireContext()).getUsername() ?: "Ti"
        names[mySlot] = myName
        names[if (mySlot == 0) 1 else 0] = opponentName
        updateScores(0, 0)
        toast("Protivnik pronađen: $opponentName")
    }

    // ---- Ko zna zna ------------------------------------------------------

    private fun onKzzQuestion(e: JSONObject) {
        binding.flipper.displayedChild = PAGE_KOZNAZNA
        kzzAnswered = false
        binding.tvKzzInfo.text = ""
        binding.tvQuestion.text = "${e.optInt("index") + 1}/${e.optInt("total", 5)}\n${e.optString("text")}"
        val answers = e.optJSONArray("answers")
        answerButtons.forEachIndexed { i, b -> b.text = answers?.optString(i) ?: ""; resetButton(b) }
        applyScores(e)
        startCountdown(e.optLong("timeMs", 5000))
    }

    private fun onAnswerClicked(index: Int) {
        if (kzzAnswered || binding.flipper.displayedChild != PAGE_KOZNAZNA) return
        kzzAnswered = true
        GameClient.sendAnswer(index)
        answerButtons.forEach { it.isEnabled = false }
        markSelected(answerButtons[index])
        binding.tvKzzInfo.text = "Tvoj odgovor je poslat"
    }

    private fun onKzzResult(e: JSONObject) {
        displayTimer?.cancel()
        val correct = e.optInt("correct", -1)
        val your = if (e.isNull("yourAnswer")) -1 else e.optInt("yourAnswer", -1)
        answerButtons.forEach { it.isEnabled = false }
        if (correct in answerButtons.indices) colorButton(answerButtons[correct], true)
        if (your in answerButtons.indices && your != correct) colorButton(answerButtons[your], false)
        applyScores(e)
        e.optJSONArray("deltas")?.let { d ->
            val mine = d.optInt(mySlot)
            binding.tvKzzInfo.text = if (mine > 0) "+$mine poena!" else if (mine < 0) "$mine poena" else "Bez poena"
        }
    }

    // ---- Spojnice --------------------------------------------------------

    private fun onSpjRound(e: JSONObject) {
        binding.flipper.displayedChild = PAGE_SPOJNICE
        selectedLeft = null
        connectedLeft.fill(false)
        usedRight.fill(false)
        binding.tvCriterion.text = e.optString("criterion")
        val left = e.optJSONArray("left"); val right = e.optJSONArray("right")
        leftButtons.forEachIndexed { i, b -> b.text = left?.optString(i) ?: ""; resetButton(b) }
        rightButtons.forEachIndexed { i, b -> b.text = right?.optString(i) ?: ""; resetButton(b) }
        applyScores(e)
        setSpjTurn(e.optInt("activePlayer", 0), e.optString("phase", "lead"))
        startCountdown(e.optLong("timeMs", 30000))
    }

    private fun onSpjTurn(e: JSONObject) {
        selectedLeft = null
        setSpjTurn(e.optInt("activePlayer", 0), e.optString("phase", "lead"))
        startCountdown(e.optLong("timeMs", 30000))
    }

    private fun setSpjTurn(activePlayer: Int, phase: String) {
        spjMyTurn = activePlayer == mySlot
        val repair = phase == "repair"
        binding.tvTurnInfo.text = when {
            spjMyTurn && repair -> "Tvoj red — popravka"
            spjMyTurn -> "Tvoj red — poveži pojmove"
            repair -> "$opponentName popravlja..."
            else -> "$opponentName povezuje..."
        }
        updateSpjEnabled()
    }

    private fun updateSpjEnabled() {
        leftButtons.forEachIndexed { i, b -> b.isEnabled = spjMyTurn && !connectedLeft[i] }
        rightButtons.forEachIndexed { i, b -> b.isEnabled = spjMyTurn && !usedRight[i] }
    }

    private fun onLeftClicked(index: Int) {
        if (!spjMyTurn || connectedLeft[index]) return
        selectedLeft?.let { if (!connectedLeft[it]) resetButton(leftButtons[it]) }
        selectedLeft = index
        markSelected(leftButtons[index])
    }

    private fun onRightClicked(index: Int) {
        if (!spjMyTurn || usedRight[index]) return
        val l = selectedLeft ?: run { toast("Prvo izaberi pojam levo"); return }
        GameClient.sendConnect(l, index)
        if (!connectedLeft[l]) resetButton(leftButtons[l])
        selectedLeft = null
    }

    private fun onSpjConnect(e: JSONObject) {
        val left = e.optInt("left", -1); val right = e.optInt("right", -1)
        val correct = e.optBoolean("correct", false)
        if (left !in leftButtons.indices || right !in rightButtons.indices) return
        if (correct) {
            colorButton(leftButtons[left], true); colorButton(rightButtons[right], true)
            connectedLeft[left] = true; usedRight[right] = true
        } else {
            colorButton(leftButtons[left], false); colorButton(rightButtons[right], false)
            handler.postDelayed({
                if (_isAlive() && !connectedLeft[left]) resetButton(leftButtons[left])
                if (_isAlive() && !usedRight[right]) resetButton(rightButtons[right])
                updateSpjEnabled()
            }, 600)
        }
        applyScores(e)
        updateSpjEnabled()
    }

    // ---- Skočko ----------------------------------------------------------

    private fun onSkockoRound(e: JSONObject) {
        binding.flipper.displayedChild = PAGE_SKOCKO
        skockoActive = e.optInt("activePlayer", 0) == mySlot
        skockoCurrent.clear()
        renderSkockoCurrent()
        val phase = e.optString("phase", "lead")
        if (phase == "lead") skockoHistory.setLength(0) else skockoHistory.append("— popravka —\n")
        binding.tvSkockoHistory.text = skockoHistory.toString()
        binding.tvSkockoTurn.text = when {
            skockoActive && phase == "steal" -> "Tvoja šansa! 1 pokušaj"
            skockoActive -> "Tvoj red — pogodi kombinaciju"
            phase == "steal" -> "$opponentName ima šansu..."
            else -> "$opponentName igra..."
        }
        applyScores(e)
        updateSkockoEnabled()
        startCountdown(e.optLong("timeMs", 30000))
    }

    private fun onSkockoSymbol(i: Int) {
        if (!skockoActive || skockoCurrent.size >= 4) return
        skockoCurrent.add(i)
        renderSkockoCurrent()
    }

    private fun renderSkockoCurrent() {
        val sb = StringBuilder()
        for (k in 0 until 4) sb.append(if (k < skockoCurrent.size) SKOCKO_SYMBOLS[skockoCurrent[k]] else "_")
        binding.tvSkockoCurrent.text = sb.toString()
    }

    private fun onSkockoSubmit() {
        if (!skockoActive || skockoCurrent.size != 4) { toast("Izaberi 4 simbola"); return }
        val arr = JSONArray(); skockoCurrent.forEach { arr.put(it) }
        GameClient.send(JSONObject().put("type", "skocko_guess").put("guess", arr))
        skockoCurrent.clear()
        renderSkockoCurrent()
    }

    private fun onSkockoFeedback(e: JSONObject) {
        val guess = e.optJSONArray("guess")
        val row = StringBuilder()
        if (guess != null) for (k in 0 until guess.length()) row.append(SKOCKO_SYMBOLS[guess.optInt(k)])
        row.append("   ⚫${e.optInt("exact")}  ⚪${e.optInt("color")}")
        if (e.optBoolean("solved")) row.append("  ✅")
        skockoHistory.append(row).append("\n")
        binding.tvSkockoHistory.text = skockoHistory.toString()
        applyScores(e)
    }

    private fun onSkockoRoundEnd(e: JSONObject) {
        val secret = e.optJSONArray("secret")
        val sb = StringBuilder("Rešenje: ")
        if (secret != null) for (k in 0 until secret.length()) sb.append(SKOCKO_SYMBOLS[secret.optInt(k)])
        skockoHistory.append(sb).append("\n")
        binding.tvSkockoHistory.text = skockoHistory.toString()
        applyScores(e)
    }

    private fun updateSkockoEnabled() {
        symbolButtons.forEach { it.isEnabled = skockoActive }
        binding.btnSkockoBack.isEnabled = skockoActive
        binding.btnSkockoSubmit.isEnabled = skockoActive
    }

    // ---- Korak po korak --------------------------------------------------

    private fun onKorakRound(e: JSONObject) {
        binding.flipper.displayedChild = PAGE_KORAK
        korakActive = e.optInt("activePlayer", 0) == mySlot
        binding.tvKorakClues.text = ""
        binding.tvKorakPoints.text = ""
        binding.etKorakGuess.setText("")
        binding.tvKorakTurn.text = if (korakActive) "Tvoj red — pogađaj" else "$opponentName pogađa..."
        binding.btnKorakSubmit.isEnabled = korakActive
        binding.etKorakGuess.isEnabled = korakActive
        applyScores(e)
        startCountdown(e.optLong("timeMs", 70000))
    }

    private fun onKorakClue(e: JSONObject) {
        val line = "${e.optInt("step")}. ${e.optString("text")}\n"
        binding.tvKorakClues.append(line)
        binding.tvKorakPoints.text = "Mogući poeni: ${e.optInt("possiblePoints")}"
    }

    private fun onKorakCorrect(e: JSONObject) {
        displayTimer?.cancel()
        binding.tvKorakTurn.text = "Tačno: ${e.optString("answer")} 🎉"
        applyScores(e)
    }

    private fun onKorakSteal(e: JSONObject) {
        korakActive = e.optInt("activePlayer", 0) == mySlot
        binding.tvKorakTurn.text = if (korakActive) "Tvoja šansa! Pogodi za 5 poena" else "$opponentName pokušava..."
        binding.btnKorakSubmit.isEnabled = korakActive
        binding.etKorakGuess.isEnabled = korakActive
        applyScores(e)
        startCountdown(e.optLong("timeMs", 10000))
    }

    private fun onKorakRoundEnd(e: JSONObject) {
        binding.tvKorakClues.append("\nRešenje: ${e.optString("answer")}\n")
        applyScores(e)
    }

    private fun onKorakSubmit() {
        if (!korakActive) return
        val text = binding.etKorakGuess.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        GameClient.send(JSONObject().put("type", "korak_guess").put("text", text))
        binding.etKorakGuess.setText("")
    }

    // ---- Moj broj --------------------------------------------------------

    private fun setupMojBroj() {
        mbNumButtons.forEachIndexed { i, b -> b.setOnClickListener { if (!mbSubmitted && i < mbNumbers.size) { mbTokens.add(mbNumbers[i].toString()); renderMbExpr() } } }
        val ops = listOf(binding.btnMbPlus to "+", binding.btnMbMinus to "-", binding.btnMbMul to "*", binding.btnMbDiv to "/", binding.btnMbLPar to "(", binding.btnMbRPar to ")")
        ops.forEach { (btn, tok) -> btn.setOnClickListener { if (!mbSubmitted) { mbTokens.add(tok); renderMbExpr() } } }
        binding.btnMbBack.setOnClickListener { if (!mbSubmitted && mbTokens.isNotEmpty()) { mbTokens.removeAt(mbTokens.size - 1); renderMbExpr() } }
        binding.btnMbSubmit.setOnClickListener { onMojBrojSubmit() }
    }

    private fun onMojBrojRound(e: JSONObject) {
        binding.flipper.displayedChild = PAGE_MOJBROJ
        val nums = e.optJSONArray("numbers")
        mbNumbers = IntArray(nums?.length() ?: 0) { nums!!.optInt(it) }
        mbNumButtons.forEachIndexed { i, b -> b.text = if (i < mbNumbers.size) mbNumbers[i].toString() else ""; b.isEnabled = true }
        binding.tvMbTarget.text = e.optInt("target").toString()
        mbTokens.clear(); mbSubmitted = false; renderMbExpr()
        binding.tvMbTurn.text = "Sastavi izraz pomoću ponuđenih brojeva"
        setMbEnabled(true)
        applyScores(e)
        startCountdown(e.optLong("timeMs", 60000))
    }

    private fun renderMbExpr() {
        binding.tvMbExpr.text = mbTokens.joinToString(" ") { t -> when (t) { "*" -> "×"; "/" -> "÷"; else -> t } }
    }

    private fun onMojBrojSubmit() {
        if (mbSubmitted) return
        mbSubmitted = true
        GameClient.send(JSONObject().put("type", "mojbroj_submit").put("expr", mbTokens.joinToString("")))
        setMbEnabled(false)
        binding.tvMbTurn.text = "Poslato, čeka se protivnik..."
    }

    private fun setMbEnabled(enabled: Boolean) {
        mbNumButtons.forEach { it.isEnabled = enabled }
        listOf(binding.btnMbPlus, binding.btnMbMinus, binding.btnMbMul, binding.btnMbDiv, binding.btnMbLPar, binding.btnMbRPar, binding.btnMbBack, binding.btnMbSubmit)
            .forEach { it.isEnabled = enabled }
    }

    private fun onMojBrojResult(e: JSONObject) {
        displayTimer?.cancel()
        val your = if (e.isNull("yourResult")) "—" else e.optInt("yourResult").toString()
        val opp = if (e.isNull("opponentResult")) "—" else e.optInt("opponentResult").toString()
        binding.tvMbTurn.text = "Cilj ${e.optInt("target")} • ti: $your • protivnik: $opp"
        applyScores(e)
    }

    // ---- Asocijacije -----------------------------------------------------

    private fun setupAsocijacije() {
        for (c in 0 until 4) for (f in 0 until 4) {
            asoFieldButtons[c][f].setOnClickListener {
                if (asoActive) GameClient.send(JSONObject().put("type", "aso_open").put("col", c).put("field", f))
            }
        }
        asoSolveButtons.forEachIndexed { c, b -> b.setOnClickListener { onAsoSolveColumn(c) } }
        binding.btnAsoFinal.setOnClickListener { onAsoSolveFinal() }
        binding.btnAsoPass.setOnClickListener { if (asoActive) GameClient.send(JSONObject().put("type", "aso_pass")) }
    }

    private fun onAsoRound(e: JSONObject) {
        binding.flipper.displayedChild = PAGE_ASO
        asoActive = e.optInt("activePlayer", 0) == mySlot
        val labels = listOf("A", "B", "C", "D")
        for (c in 0 until 4) {
            asoColHeaders[c].text = labels[c]
            for (f in 0 until 4) { asoFieldButtons[c][f].text = "?"; asoFieldButtons[c][f].isEnabled = asoActive }
        }
        binding.etAsoGuess.setText("")
        setAsoTurnText()
        setAsoControlsEnabled()
        applyScores(e)
        startCountdown(e.optLong("timeMs", 120000))
    }

    private fun onAsoField(e: JSONObject) {
        val c = e.optInt("col"); val f = e.optInt("field")
        if (c in 0 until 4 && f in 0 until 4) {
            asoFieldButtons[c][f].text = e.optString("text")
            asoFieldButtons[c][f].isEnabled = false
        }
        applyScores(e)
    }

    private fun onAsoSolved(e: JSONObject) {
        if (e.optString("target") == "column") {
            val c = e.optInt("col")
            if (c in 0 until 4) { asoColHeaders[c].text = e.optString("text"); asoSolveButtons[c].isEnabled = false }
            toast("Kolona rešena (+${e.optInt("points")})")
        } else {
            toast("Konačno rešenje! +${e.optInt("points")}")
        }
        applyScores(e)
    }

    private fun onAsoTurn(e: JSONObject) {
        asoActive = e.optInt("activePlayer", 0) == mySlot
        setAsoTurnText()
        setAsoControlsEnabled()
    }

    private fun onAsoRoundEnd(e: JSONObject) {
        val sols = e.optJSONArray("columnSolutions")
        if (sols != null) for (c in 0 until minOf(4, sols.length())) asoColHeaders[c].text = sols.optString(c)
        binding.tvAsoTurn.text = "Konačno: ${e.optString("finalSolution")}"
        applyScores(e)
    }

    private fun onAsoSolveColumn(c: Int) {
        if (!asoActive) return
        val text = binding.etAsoGuess.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) { toast("Upiši rešenje kolone"); return }
        GameClient.send(JSONObject().put("type", "aso_guess").put("target", "column").put("col", c).put("text", text))
        binding.etAsoGuess.setText("")
    }

    private fun onAsoSolveFinal() {
        if (!asoActive) return
        val text = binding.etAsoGuess.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) { toast("Upiši konačno rešenje"); return }
        GameClient.send(JSONObject().put("type", "aso_guess").put("target", "final").put("text", text))
        binding.etAsoGuess.setText("")
    }

    private fun setAsoTurnText() {
        binding.tvAsoTurn.text = if (asoActive) "Tvoj red — otvori polje, pa pogađaj" else "$opponentName je na potezu..."
    }

    private fun setAsoControlsEnabled() {
        for (c in 0 until 4) for (f in 0 until 4) {
            val stillClosed = asoFieldButtons[c][f].text?.toString() == "?"
            asoFieldButtons[c][f].isEnabled = asoActive && stillClosed
        }
        asoSolveButtons.forEach { it.isEnabled = asoActive }
        binding.btnAsoFinal.isEnabled = asoActive
        binding.btnAsoPass.isEnabled = asoActive
    }

    // ---- match lifecycle / helpers --------------------------------------

    private fun onGameComplete(e: JSONObject) {
        e.optJSONArray("totalScores")?.let { toast("Kraj igre — ukupno ${it.optInt(0)} : ${it.optInt(1)}") }
    }

    private fun onMatchOver(e: JSONObject) {
        matchOver = true
        displayTimer?.cancel()
        binding.flipper.displayedChild = PAGE_RESULT
        val youWon = e.optBoolean("youWon", false); val tie = e.optBoolean("tie", false)
        binding.tvResultTitle.text = if (tie) "NEREŠENO" else if (youWon) "POBEDA!" else "PORAZ"
        val totals = e.optJSONArray("totalScores")
        binding.tvResultScore.text = "${names[0]}: ${totals?.optInt(0) ?: 0}    ${names[1]}: ${totals?.optInt(1) ?: 0}"
        val stars = e.optInt("starsDelta", 0)
        binding.tvResultStars.text = "Zvezde: ${if (stars >= 0) "+" else ""}$stars"
    }

    private fun applyScores(e: JSONObject) {
        e.optJSONArray("scores")?.let { updateScores(it.optInt(0), it.optInt(1)) }
    }

    private fun updateScores(a: Int, b: Int) {
        binding.gameHeader.tvPlayer1Score.text = "${names[0]}\n$a"
        binding.gameHeader.tvPlayer2Score.text = "${names[1]}\n$b"
    }

    private fun startCountdown(ms: Long) {
        displayTimer?.cancel()
        binding.gameHeader.tvGameTimer.text = ceil(ms / 1000.0).toInt().toString()
        displayTimer = object : CountDownTimer(ms, 250) {
            override fun onTick(msLeft: Long) { binding.gameHeader.tvGameTimer.text = ceil(msLeft / 1000.0).toInt().toString() }
            override fun onFinish() { binding.gameHeader.tvGameTimer.text = "0" }
        }.start()
    }

    private fun resetButton(b: MaterialButton) {
        b.backgroundTintList = defaultTint; b.setTextColor(defaultTextColor); b.isEnabled = true
    }

    private fun colorButton(b: MaterialButton, correct: Boolean) {
        val res = if (correct) android.R.color.holo_green_light else android.R.color.holo_red_light
        b.backgroundTintList = ContextCompat.getColorStateList(requireContext(), res)
        b.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
    }

    private fun markSelected(b: MaterialButton) {
        val tv = TypedValue()
        requireContext().theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, tv, true)
        b.backgroundTintList = ColorStateList.valueOf(tv.data)
        b.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
    }

    private fun _isAlive(): Boolean = view != null && isAdded

    private fun toast(msg: String) { if (_isAlive()) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show() }

    private fun leaveToMenu() { findNavController().popBackStack() }

    override fun onDestroyView() {
        GameClient.removeListener(listener)
        displayTimer?.cancel()
        handler.removeCallbacksAndMessages(null)
        if (!matchOver) GameClient.sendLeaveMatch()
        super.onDestroyView()
    }
}
