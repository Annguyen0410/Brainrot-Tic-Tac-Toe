package com.example.myapplicationnnn

import android.animation.ObjectAnimator
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    // UI Components - Main Menu
    private lateinit var mainMenuScreen: LinearLayout
    private lateinit var gameContentScrollView: ScrollView
    private lateinit var playerNameInput: TextInputEditText
    private lateinit var playWithBotButton: Button
    private lateinit var playWithPlayerButton: Button
    private lateinit var settingsButton: ImageView

    // UI Components - Game (existing)
    private lateinit var titleText: TextView
    private lateinit var coinCount: TextView
    private lateinit var playerSelectionCard: CardView
    private lateinit var skinSelectionCard: CardView
    private lateinit var gameBoardCard: CardView
    private lateinit var chooseXButton: Button
    private lateinit var chooseOButton: Button
    private lateinit var skinRecyclerView: RecyclerView
    private lateinit var confirmSkinButton: Button
    private lateinit var gameBoard: GridLayout
    private lateinit var currentTurnText: TextView
    private lateinit var player1SkinImage: ImageView
    private lateinit var player2SkinImage: ImageView
    private lateinit var player1NameText: TextView
    private lateinit var player2NameText: TextView
    private lateinit var scoreText: TextView
    private lateinit var resetButton: Button
    private lateinit var newGameButton: Button
    private lateinit var playerSelectionTitle: TextView
    private lateinit var skinSelectionTitle: TextView


    // Game State
    private val board = Array(BOARD_SIZE) { Array(BOARD_SIZE) { 0 } }
    private var currentPlayer = 1 // HUMAN_PLAYER_NUMBER
    private var gameActive = false
    private var player1Wins = 0
    private var player2Wins = 0
    private var isPlayer1X = true
    private var player1UIName = "Player 1"
    private var isAgainstBot = false


    private val cells = Array(BOARD_SIZE) { arrayOfNulls<ImageButton>(BOARD_SIZE) }

    // Skin System
    private lateinit var sharedPreferences: SharedPreferences
    private var coins = 0
    private var selectedSkinForPlayer1: Skin? = null
    private var player1Skin: Skin? = null
    private var player2Skin: Skin? = null
    private lateinit var availableSkins: MutableList<Skin>
    private lateinit var skinAdapter: SkinAdapter

    // Music & Settings
    private var mediaPlayer: MediaPlayer? = null
    private var isMusicOn: Boolean = true
    private var currentTheme: Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM


    companion object {
        private const val BOARD_SIZE = 12
        private const val WIN_LENGTH = 5
        private const val PREFS_NAME = "BrainrotTicTacToePrefs"
        private const val KEY_COINS = "coins"
        private const val KEY_SKIN_UNLOCKED_PREFIX = "skin_unlocked_"
        private const val KEY_PLAYER1_WINS = "player1_wins"
        private const val KEY_PLAYER2_WINS = "player2_wins"
        private const val KEY_PLAYER1_NAME = "player1_name"
        private const val KEY_MUSIC_ON = "music_on"
        private const val KEY_APP_THEME = "app_theme"
        private const val COINS_PER_WIN = 25
        private const val BOT_PLAYER_NUMBER = 2 // Also used for Player 2 in PvP
        private const val HUMAN_PLAYER_NUMBER = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        loadSettings()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeData()
        initializeViews()
        loadGameData()
        setupSkinSystem()
        setupClickListeners()

        initMusic()

        updateCoinDisplay()
        updateScoreDisplay()

        showMainMenu()
    }

    private fun loadSettings() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isMusicOn = sharedPreferences.getBoolean(KEY_MUSIC_ON, true)
        currentTheme = sharedPreferences.getInt(KEY_APP_THEME, AppCompatDelegate.MODE_NIGHT_NO)
        AppCompatDelegate.setDefaultNightMode(currentTheme)
    }

    private fun saveSettings() {
        with(sharedPreferences.edit()) {
            putBoolean(KEY_MUSIC_ON, isMusicOn)
            putInt(KEY_APP_THEME, currentTheme)
            apply()
        }
    }

    private fun initializeData() {
        availableSkins = mutableListOf(
            Skin(0, "Shark", R.drawable.shark_with_shoes, 0, true),
            Skin(1, "Hotdog", R.drawable.hotdog_character, 0, true),
            Skin(2, "Gigachad", R.drawable.gigachad, 50),
            Skin(3, "Pepe", R.drawable.pepe_frog, 100),
            Skin(4, "Trollface", R.drawable.trollface, 150),
            Skin(5, "Doge", R.drawable.doge, 200),
            Skin(6, "Wojak", R.drawable.wojak, 250),
            Skin(7, "Sigma", R.drawable.sigma_male, 300),
            Skin(8, "Chad", R.drawable.chad_yes, 350),
            Skin(9, "NPC", R.drawable.npc_face, 400)
        ).map { skin ->
            skin.copy(isUnlocked = sharedPreferences.getBoolean("${KEY_SKIN_UNLOCKED_PREFIX}${skin.id}", skin.price == 0))
        }.toMutableList()
    }

    private fun initializeViews() {
        mainMenuScreen = findViewById(R.id.mainMenuScreen)
        gameContentScrollView = findViewById(R.id.gameContentScrollView)
        playerNameInput = findViewById(R.id.playerNameInput)
        playWithBotButton = findViewById(R.id.playWithBotButton)
        playWithPlayerButton = findViewById(R.id.playWithPlayerButton)
        settingsButton = findViewById(R.id.settingsButton)

        titleText = findViewById(R.id.titleText)
        coinCount = findViewById(R.id.coinCount)
        playerSelectionCard = findViewById(R.id.playerSelectionCard)
        skinSelectionCard = findViewById(R.id.skinSelectionCard)
        gameBoardCard = findViewById(R.id.gameBoardCard)
        chooseXButton = findViewById(R.id.chooseXButton)
        chooseOButton = findViewById(R.id.chooseOButton)
        skinRecyclerView = findViewById(R.id.skinRecyclerView)
        confirmSkinButton = findViewById(R.id.confirmSkinButton)
        gameBoard = findViewById(R.id.gameBoard)
        currentTurnText = findViewById(R.id.currentTurnText)
        player1SkinImage = findViewById(R.id.player1SkinImage)
        player2SkinImage = findViewById(R.id.player2SkinImage)
        player1NameText = findViewById(R.id.player1Name)
        player2NameText = findViewById(R.id.player2Name)
        scoreText = findViewById(R.id.scoreText)
        resetButton = findViewById(R.id.resetButton)
        newGameButton = findViewById(R.id.newGameButton)
        playerSelectionTitle = findViewById(R.id.playerSelectionTitle)
        skinSelectionTitle = findViewById(R.id.skinSelectionTitle)
    }

    private fun loadGameData() {
        coins = sharedPreferences.getInt(KEY_COINS, 0)
        player1Wins = sharedPreferences.getInt(KEY_PLAYER1_WINS, 0)
        player2Wins = sharedPreferences.getInt(KEY_PLAYER2_WINS, 0)
        player1UIName = sharedPreferences.getString(KEY_PLAYER1_NAME, "Player 1") ?: "Player 1"
        playerNameInput.setText(player1UIName)
    }

    private fun saveGameData() {
        with(sharedPreferences.edit()) {
            putInt(KEY_COINS, coins)
            putInt(KEY_PLAYER1_WINS, player1Wins)
            putInt(KEY_PLAYER2_WINS, player2Wins)
            putString(KEY_PLAYER1_NAME, player1UIName)
            availableSkins.forEach { skin ->
                putBoolean("${KEY_SKIN_UNLOCKED_PREFIX}${skin.id}", skin.isUnlocked)
            }
            apply()
        }
    }

    private fun setupSkinSystem() {
        skinAdapter = SkinAdapter(availableSkins, coins) { skin ->
            selectedSkinForPlayer1 = skin
        }
        skinRecyclerView.layoutManager = GridLayoutManager(this, 3)
        skinRecyclerView.adapter = skinAdapter
        selectedSkinForPlayer1 = availableSkins.firstOrNull { it.isUnlocked } ?: availableSkins.first()
        val defaultIndex = availableSkins.indexOf(selectedSkinForPlayer1)
        if (defaultIndex != -1) {
            skinAdapter.setSelectedSkin(defaultIndex)
        }
    }

    private fun setupClickListeners() {
        settingsButton.setOnClickListener { showSettingsDialog() }

        playWithBotButton.setOnClickListener {
            isAgainstBot = true
            player1UIName = playerNameInput.text.toString().ifBlank { "Player 1" }
            saveGameData()
            animateButton(playWithBotButton)
            hideMainMenuAndShowGameContent()
            showPlayerSymbolSelection()
        }

        playWithPlayerButton.setOnClickListener {
            isAgainstBot = false
            player1UIName = playerNameInput.text.toString().ifBlank { "Player 1" }
            saveGameData()
            animateButton(playWithPlayerButton)
            hideMainMenuAndShowGameContent()
            showPlayerSymbolSelection()
        }

        chooseXButton.setOnClickListener {
            isPlayer1X = true
            animateButton(chooseXButton)
            showSkinSelectionForPlayer1()
        }

        chooseOButton.setOnClickListener {
            isPlayer1X = false
            animateButton(chooseOButton)
            showSkinSelectionForPlayer1()
        }

        confirmSkinButton.setOnClickListener {
            animateButton(confirmSkinButton)
            selectedSkinForPlayer1?.let { skin ->
                if (skin.isUnlocked || skin.price == 0) {
                    player1Skin = skin
                    finalizePlayer2AndStartGame()
                } else if (coins >= skin.price) {
                    coins -= skin.price
                    unlockSkinInList(skin.id)
                    saveGameData()
                    updateCoinDisplay()
                    refreshSkinAdapterData()
                    player1Skin = availableSkins.find { it.id == skin.id }
                    finalizePlayer2AndStartGame()
                } else {
                    Toast.makeText(this, "Not enough coins to buy ${skin.name}!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        resetButton.setOnClickListener {
            resetBoard()
            animateButton(resetButton)
        }

        newGameButton.setOnClickListener {
            animateButton(newGameButton)
            showMainMenu()
            resetFullGameStats()
        }
    }

    private fun finalizePlayer2AndStartGame() {
        if (isAgainstBot) {
            player2Skin = availableSkins.filter { it.isUnlocked && it.id != player1Skin?.id }.shuffled().firstOrNull()
            if (player2Skin == null) {
                player2Skin = availableSkins.filter { it.isUnlocked }.shuffled().firstOrNull()
            }
            if (player2Skin == null) {
                player2Skin = availableSkins.firstOrNull()
            }
        } else {
            assignPlayer2Skin()
        }
        startGame()
    }

    private fun showMainMenu() {
        mainMenuScreen.visibility = View.VISIBLE
        gameContentScrollView.visibility = View.GONE
        playerSelectionCard.visibility = View.GONE
        skinSelectionCard.visibility = View.GONE
        gameBoardCard.visibility = View.GONE
    }

    private fun hideMainMenuAndShowGameContent() {
        mainMenuScreen.visibility = View.GONE
        gameContentScrollView.visibility = View.VISIBLE
    }

    private fun showPlayerSymbolSelection() {
        playerSelectionTitle.text = "$player1UIName, Choose Your Symbol"
        playerSelectionCard.visibility = View.VISIBLE
        skinSelectionCard.visibility = View.GONE
        gameBoardCard.visibility = View.GONE
    }

    private fun showSkinSelectionForPlayer1() {
        playerSelectionCard.visibility = View.GONE
        skinSelectionTitle.text = "$player1UIName, Select Your Skin"
        skinSelectionCard.visibility = View.VISIBLE
        gameBoardCard.visibility = View.GONE
        refreshSkinAdapterData()
    }

    private fun assignPlayer2Skin() {
        val p1SkinId = player1Skin?.id
        player2Skin = availableSkins.filter { it.isUnlocked && it.id != p1SkinId }.shuffled().firstOrNull()
        if (player2Skin == null) {
            player2Skin = availableSkins.filter { it.id != p1SkinId }.shuffled().firstOrNull()
        }
        if (player2Skin == null) {
            player2Skin = availableSkins.firstOrNull { it.id != p1SkinId } ?: player1Skin
        }
    }

    private fun unlockSkinInList(skinId: Int) {
        val skinIndex = availableSkins.indexOfFirst { it.id == skinId }
        if (skinIndex != -1) {
            availableSkins[skinIndex] = availableSkins[skinIndex].copy(isUnlocked = true)
        }
    }

    private fun refreshSkinAdapterData() {
        skinAdapter.updateSkins(availableSkins, coins)
    }

    private fun startGame() {
        skinSelectionCard.visibility = View.GONE
        gameBoardCard.visibility = View.VISIBLE
        setupGameBoard()
        updatePlayerInfo() // Call before resetBoard to ensure player names/skins are set
        resetBoard()       // resetBoard will then call updateCurrentTurnDisplay
    }

    private fun setupGameBoard() {
        gameBoard.removeAllViews()
        gameBoard.rowCount = BOARD_SIZE
        gameBoard.columnCount = BOARD_SIZE
        val cellSizeInDp = 32
        val cellSizeInPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, cellSizeInDp.toFloat(), resources.displayMetrics).toInt()

        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                val cell = ImageButton(this)
                val params = GridLayout.LayoutParams()
                params.width = cellSizeInPx
                params.height = cellSizeInPx
                params.rowSpec = GridLayout.spec(row)
                params.columnSpec = GridLayout.spec(col)
                params.setMargins(1, 1, 1, 1)
                cell.layoutParams = params
                cell.setBackgroundResource(R.drawable.cell_background)
                cell.scaleType = ImageView.ScaleType.FIT_CENTER
                cell.setPadding(6,6,6,6)
                cell.setOnClickListener {
                    // Pass HUMAN_PLAYER_NUMBER because it's a direct click by the human user.
                    // onCellClicked will use 'currentPlayer' to determine the actual player.
                    onCellClicked(row, col, HUMAN_PLAYER_NUMBER)
                }
                gameBoard.addView(cell)
                cells[row][col] = cell
            }
        }
    }

    private fun onCellClicked(row: Int, col: Int, playerMakingMove: Int) { // playerMakingMove is mostly for context, actual player is 'currentPlayer'
        if (!gameActive || board[row][col] != 0) {
            return
        }

        val actualPlayerThisTurn = currentPlayer
        board[row][col] = actualPlayerThisTurn
        val currentPlayerSkinToSet = if (actualPlayerThisTurn == HUMAN_PLAYER_NUMBER) player1Skin else player2Skin
        cells[row][col]?.setImageResource(currentPlayerSkinToSet?.imageResource ?: R.drawable.ic_launcher_foreground)
        cells[row][col]?.isEnabled = false

        if (checkForWin(row, col, actualPlayerThisTurn)) {
            handleWin(actualPlayerThisTurn)
        } else if (isBoardFull()) {
            handleDraw()
        } else {
            if (actualPlayerThisTurn == HUMAN_PLAYER_NUMBER) { // Player 1 (Human) just moved
                currentPlayer = BOT_PLAYER_NUMBER // Represents Player 2 in PvP or Bot
                updateCurrentTurnDisplay()
                if (isAgainstBot && gameActive) {
                    setBoardEnabled(false)
                    Handler(Looper.getMainLooper()).postDelayed({
                        performBotMove()
                        setBoardEnabled(true)
                    }, 150L)
                }
            } else { // Bot OR Player 2 just moved
                currentPlayer = HUMAN_PLAYER_NUMBER
                updateCurrentTurnDisplay()
            }
        }
    }

    private fun setBoardEnabled(isEnabled: Boolean) {
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                if (board[r][c] == 0) {
                    cells[r][c]?.isEnabled = isEnabled
                    cells[r][c]?.alpha = if (isEnabled) 1.0f else 0.7f
                }
            }
        }
    }

    // --- SMARTER BOT LOGIC ---
    private fun performBotMove() {
        if (!gameActive) return

        val bestMove = findBestMoveForBot()

        if (bestMove != null) {
            onCellClicked(bestMove.first, bestMove.second, BOT_PLAYER_NUMBER) // Pass BOT_PLAYER_NUMBER for context if needed, though 'currentPlayer' logic in onCellClicked handles it
        } else {
            val emptyCells = getEmptyCells()
            if (emptyCells.isNotEmpty()) {
                val randomMove = emptyCells.random()
                onCellClicked(randomMove.first, randomMove.second, BOT_PLAYER_NUMBER)
            }
        }
    }

    private fun findBestMoveForBot(): Pair<Int, Int>? {
        val emptyCells = getEmptyCells()
        if (emptyCells.isEmpty()) return null

        var bestScore = Int.MIN_VALUE
        var move: Pair<Int, Int>? = null
        val tempBoard = board.map { it.clone() }.toTypedArray()

        for (cell in emptyCells) {
            tempBoard[cell.first][cell.second] = BOT_PLAYER_NUMBER
            val score = evaluateBoardState(tempBoard, 0, false) // Next turn is human (minimizing for bot)
            tempBoard[cell.first][cell.second] = 0

            if (score > bestScore) {
                bestScore = score
                move = cell
            }
        }
        return move
    }

    private fun evaluateBoardState(currentBoard: Array<Array<Int>>, depth: Int, isMaximizingPlayer: Boolean): Int {
        if (hasPlayerWon(currentBoard, BOT_PLAYER_NUMBER)) return 1000 - depth
        if (hasPlayerWon(currentBoard, HUMAN_PLAYER_NUMBER)) return -1000 + depth
        if (getEmptyCells(currentBoard).isEmpty()) return 0

        val maxDepth = 1
        if (depth >= maxDepth) {
            // When depth limit is reached, evaluate from bot's perspective
            return scoreBoardHeuristically(currentBoard, BOT_PLAYER_NUMBER) - scoreBoardHeuristically(currentBoard, HUMAN_PLAYER_NUMBER)
        }

        val emptyCells = getEmptyCells(currentBoard)

        if (isMaximizingPlayer) { // Bot's turn (simulated) -> wants to maximize this value
            var maxEval = Int.MIN_VALUE
            for (cell in emptyCells) {
                currentBoard[cell.first][cell.second] = BOT_PLAYER_NUMBER
                val eval = evaluateBoardState(currentBoard, depth + 1, false) // Next is human's turn
                currentBoard[cell.first][cell.second] = 0
                maxEval = max(maxEval, eval)
            }
            return maxEval
        } else { // Human's turn (simulated) -> bot wants human to get minimal score from bot's perspective
            var minEval = Int.MAX_VALUE
            for (cell in emptyCells) {
                currentBoard[cell.first][cell.second] = HUMAN_PLAYER_NUMBER
                val eval = evaluateBoardState(currentBoard, depth + 1, true) // Next is bot's turn
                currentBoard[cell.first][cell.second] = 0
                minEval = min(minEval, eval)
            }
            return minEval
        }
    }

    private fun scoreBoardHeuristically(currentBoard: Array<Array<Int>>, player: Int): Int {
        var score = 0
        // val opponent = if (player == BOT_PLAYER_NUMBER) HUMAN_PLAYER_NUMBER else BOT_PLAYER_NUMBER // Not used in this simplified heuristic
        val lineScores = intArrayOf(0, 1, 10, 100, 500, 10000) // Scores for 0, 1, 2, 3, 4, 5 in a row (5 is win)

        val checkDirections = fun(startR: Int, startC: Int, dR: Int, dC: Int) {
            var playerCount = 0
            var opponentCount = 0 // To check if the line is blocked
            for (k in 0 until WIN_LENGTH) {
                val r = startR + k * dR
                val c = startC + k * dC
                if (r !in 0 until BOARD_SIZE || c !in 0 until BOARD_SIZE) { // Off board
                    playerCount = -1 // Invalidate this line
                    break
                }
                when (currentBoard[r][c]) {
                    player -> playerCount++
                    0 -> {} // Empty
                    else -> opponentCount++ // Opponent's piece
                }
            }
            if (playerCount != -1 && opponentCount == 0 && playerCount > 0) { // Only score unblocked lines
                score += lineScores[playerCount]
            }
        }

        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                if (c <= BOARD_SIZE - WIN_LENGTH) checkDirections(r, c, 0, 1)  // Horizontal
                if (r <= BOARD_SIZE - WIN_LENGTH) checkDirections(r, c, 1, 0)  // Vertical
                if (r <= BOARD_SIZE - WIN_LENGTH && c <= BOARD_SIZE - WIN_LENGTH) checkDirections(r, c, 1, 1) // Diagonal \
                if (r <= BOARD_SIZE - WIN_LENGTH && c >= WIN_LENGTH - 1) checkDirections(r, c, 1, -1) // Diagonal /
            }
        }
        return score
    }

    private fun hasPlayerWon(currentBoard: Array<Array<Int>>, player: Int): Boolean {
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 .. BOARD_SIZE - WIN_LENGTH) { // Horizontal
                if ((0 until WIN_LENGTH).all { currentBoard[r][c + it] == player }) return true
            }
        }
        for (c in 0 until BOARD_SIZE) {
            for (r in 0 .. BOARD_SIZE - WIN_LENGTH) { // Vertical
                if ((0 until WIN_LENGTH).all { currentBoard[r + it][c] == player }) return true
            }
        }
        for (r in 0 .. BOARD_SIZE - WIN_LENGTH) {
            for (c in 0 .. BOARD_SIZE - WIN_LENGTH) { // Diagonal \
                if ((0 until WIN_LENGTH).all { currentBoard[r + it][c + it] == player }) return true
            }
        }
        for (r in 0 .. BOARD_SIZE - WIN_LENGTH) {
            for (c in WIN_LENGTH - 1 until BOARD_SIZE) { // Diagonal /
                if ((0 until WIN_LENGTH).all { currentBoard[r + it][c - it] == player }) return true
            }
        }
        return false
    }

    private fun getEmptyCells(currentBoard: Array<Array<Int>>): List<Pair<Int, Int>> {
        val cellsList = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                if (currentBoard[r][c] == 0) cellsList.add(Pair(r, c))
            }
        }
        return cellsList
    }

    private fun getEmptyCells(): List<Pair<Int, Int>> { // For the main game board
        return getEmptyCells(this.board)
    }
    // --- END SMARTER BOT LOGIC ---

    private fun checkForWin(lastRow: Int, lastCol: Int, player: Int): Boolean {
        val directions = arrayOf(Pair(0, 1), Pair(1, 0), Pair(1, 1), Pair(1, -1))
        for (dir in directions) {
            var count = 1
            for (i in 1 until WIN_LENGTH) {
                val r = lastRow + dir.first * i; val c = lastCol + dir.second * i
                if (r in 0 until BOARD_SIZE && c in 0 until BOARD_SIZE && board[r][c] == player) count++ else break
            }
            for (i in 1 until WIN_LENGTH) {
                val r = lastRow - dir.first * i; val c = lastCol - dir.second * i
                if (r in 0 until BOARD_SIZE && c in 0 until BOARD_SIZE && board[r][c] == player) count++ else break
            }
            if (count >= WIN_LENGTH) return true
        }
        return false
    }

    private fun isBoardFull(): Boolean {
        return getEmptyCells().isEmpty()
    }

    private fun handleWin(winner: Int) {
        gameActive = false
        val winnerDisplayName: String; val winnerSymbol: String

        if (winner == HUMAN_PLAYER_NUMBER) {
            winnerDisplayName = player1UIName
            winnerSymbol = if (isPlayer1X) "X" else "O"
            player1Wins++
            coins += COINS_PER_WIN
            updateCoinDisplay(); saveGameData()
            showGameEndDialog("$winnerDisplayName ($winnerSymbol) Wins! You earned $COINS_PER_WIN coins! 🪙")
        } else { // Bot or Player 2 wins
            winnerDisplayName = if (isAgainstBot) "BOT" else "Player 2"
            winnerSymbol = if (isPlayer1X) "O" else "X" // P2/Bot is opposite of P1
            player2Wins++
            saveGameData()
            showGameEndDialog("$winnerDisplayName ($winnerSymbol) Wins!")
        }
        updateScoreDisplay()
    }

    private fun handleDraw() {
        gameActive = false
        showGameEndDialog("It's a Draw! 🤝")
    }

    private fun showGameEndDialog(message: String) {
        AlertDialog.Builder(this, R.style.AlertDialogCustom)
            .setTitle("Game Over")
            .setMessage(message)
            .setPositiveButton("Play Again") { dialog, _ -> resetBoard(); dialog.dismiss() }
            .setNegativeButton("New Game") { dialog, _ -> showMainMenu(); resetFullGameStats(); dialog.dismiss() }
            .setCancelable(false)
            .show()
    }

    private fun resetBoard() {
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                board[r][c] = 0
                cells[r][c]?.setImageResource(0); cells[r][c]?.isEnabled = true; cells[r][c]?.alpha = 1.0f
            }
        }
        currentPlayer = HUMAN_PLAYER_NUMBER
        gameActive = true
        updateCurrentTurnDisplay()
        // No automatic bot move here, that's handled after P1's move in onCellClicked
    }

    private fun resetFullGameStats() {
        player1Wins = 0; player2Wins = 0
        saveGameData(); updateScoreDisplay()
    }

    private fun updatePlayerInfo() {
        val p1Symbol = if (isPlayer1X) "X" else "O"
        val p2Symbol = if (isPlayer1X) "O" else "X"

        player1NameText.text = "$player1UIName ($p1Symbol)"
        player2NameText.text = (if (isAgainstBot) "BOT" else "Player 2") + " ($p2Symbol)"

        player1Skin?.let { player1SkinImage.setImageResource(it.imageResource) }
        player2Skin?.let { player2SkinImage.setImageResource(it.imageResource) }
    }

    private fun updateCurrentTurnDisplay() {
        val currentSymbol: String; val currentPlayerDisplayName: String

        if (currentPlayer == HUMAN_PLAYER_NUMBER) { // Player 1's turn
            currentPlayerDisplayName = player1UIName
            currentSymbol = if (isPlayer1X) "X" else "O"
            player1NameText.setTextColor(ContextCompat.getColor(this, R.color.accent_gold)); player1SkinImage.alpha = 1.0f
            player2NameText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary)); player2SkinImage.alpha = 0.5f
        } else { // Player 2's turn (or Bot)
            currentPlayerDisplayName = if (isAgainstBot) "BOT" else "Player 2"
            currentSymbol = if (isPlayer1X) "O" else "X" // P2/Bot is opposite of P1
            player2NameText.setTextColor(ContextCompat.getColor(this, R.color.accent_gold)); player2SkinImage.alpha = 1.0f
            player1NameText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary)); player1SkinImage.alpha = 0.5f
        }
        currentTurnText.text = "Turn: $currentPlayerDisplayName ($currentSymbol)"
    }

    private fun updateScoreDisplay() {
        scoreText.text = "Score: $player1Wins - $player2Wins"
    }

    private fun updateCoinDisplay() {
        coinCount.text = coins.toString()
    }

    private fun animateButton(button: Button) {
        ObjectAnimator.ofFloat(button, "alpha", 0.5f, 1.0f).apply {
            duration = 150; interpolator = DecelerateInterpolator(); start()
        }
    }

    private fun initMusic() {
        if (mediaPlayer == null) {
            try {
                mediaPlayer = MediaPlayer.create(this, R.raw.background_music)
                mediaPlayer?.isLooping = true
            } catch (e: Exception) {
                // Log error or show a toast if music file fails to load
                // For example: Toast.makeText(this, "Error loading music", Toast.LENGTH_SHORT).show()
                mediaPlayer = null // Ensure it's null if creation failed
            }
        }
        if (isMusicOn) playMusic()
    }

    private fun playMusic() {
        if (isMusicOn && mediaPlayer?.isPlaying == false) {
            try {
                mediaPlayer?.start()
            } catch (e: IllegalStateException) {
                // Handle potential error if media player is in a bad state
                mediaPlayer = null // Reset it
                initMusic() // Try to reinitialize
                if (isMusicOn && mediaPlayer?.isPlaying == false) mediaPlayer?.start()
            }
        }
    }

    private fun pauseMusic() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    private fun releaseMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onResume() {
        super.onResume()
        if (mediaPlayer == null && isMusicOn) { // Re-initialize if was released and should be on
            initMusic()
        } else if (isMusicOn) {
            playMusic()
        }
    }

    override fun onPause() {
        super.onPause()
        if (!isChangingConfigurations) {
            pauseMusic()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMusic()
    }

    private fun showSettingsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)
        val switchDarkMode = dialogView.findViewById<SwitchMaterial>(R.id.switchDarkMode)
        val switchMusic = dialogView.findViewById<SwitchMaterial>(R.id.switchMusic)
        val closeButton = dialogView.findViewById<Button>(R.id.closeSettingsButton)

        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        switchDarkMode.isChecked = nightModeFlags == Configuration.UI_MODE_NIGHT_YES
        switchMusic.isChecked = isMusicOn

        val dialog = AlertDialog.Builder(this, R.style.AlertDialogCustom).setView(dialogView).setCancelable(true).create()

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            currentTheme = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(currentTheme)
            saveSettings()
            // For a non-disruptive theme change, you might avoid recreate() or handle it more gracefully.
            // recreate() // This will apply the theme but restart the activity.
        }
        switchMusic.setOnCheckedChangeListener { _, isChecked ->
            isMusicOn = isChecked
            if (isMusicOn) {
                if(mediaPlayer == null) initMusic() else playMusic()
            } else {
                pauseMusic()
            }
            saveSettings()
        }
        closeButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}