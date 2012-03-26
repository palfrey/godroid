package de.agrothe.go;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import static de.agrothe.go.MainActivity.MainCommand;
import static de.agrothe.go.MainActivity._LOG_TAG;
import static de.agrothe.go.MainActivity.playGTP;
import de.agrothe.util.Generics;
import static de.agrothe.util.Logging.isEnabledFor;
import static de.agrothe.util.Logging.log;

public
class Gtp
{
private static final
String
	_BLACK = "black",
	_WHITE = "white",
	_PASS = "PASS",
	_RESIGN = "resign",
	_BLACK_TERRITORY = "black_territory",
	_WHITE_TERRITORY = "white_territory",
	_DEAD = "dead";

private static
enum Command
{
	DRAW_BOARD,
	INIT_BOARD,
	PLAY_MOVE,
	NEW_GAME,
	CHANGE_GAME,
	HIDE_WAIT_PROGRESS,
	UNDO_MOVE,
	REDO_MOVE,
	SAVE_GAME,
	LOAD_GAME;

	int _cmd;
}
private static final
Map <Integer, Command> _cmdMessagesMap;
static
{
	final Command[] values = Command.values ();
	final Map <Integer, Command> cmdMessagesMap =
		_cmdMessagesMap = Generics.newHashMap (values.length);
	int numMessage = 0;
	for (final Command message : values)
	{
		cmdMessagesMap.put (message._cmd = numMessage++, message);
	}
}

private static
enum GtpCommand
{
	SET_BOARDSIZE ("boardsize "),
	SET_LEVEL ("level "),
	SET_KOMI ("komi "),
	SHOWBOARD ("showboard"),
	PLAY_MOVE ("play "),
	GEN_MOVE ("genmove "),
	LIST_STONES ("list_stones "),
	GET_CAPTURES ("captures "),
	LIST_LEGAL ("all_legal "),
	LIST_FINAL_STATUS ("final_status_list "),
	ESTIMATE_SCORE ("estimate_score"),
	SAVE_SGF ("printsgf "),
	LOAD_SGF ("loadsgf "),
	GET_BOARDSIZE ("query_boardsize"),
	SET_HANDICAP ("fixed_handicap "),
	UNDO ("gg-undo ");

	final
	String _gtpCommand;

	GtpCommand (
		final String pCommand
		)
	{
		_gtpCommand = pCommand;
	}
}

private static final
String
	_POSITION_LETTERS_STR = "ABCDEFGHJKLMNOPQRST",
	_ESTIMIATED_SCORE_PATTERN = "^(B|W)(.[0-9]+\\.[0-9]+)",
	_SGF_VALUE_PATTERN_STRING = "\\[([^]]+)\\]",
	_SGF_COMMENT_PATTERN_STRING = "C",
	_SGF_COLOR_2_PLAY_PATTERN_STRING = "PL",
	_SGF_COLOR_2_PLAY_WHITE = "W",
	_SGF_COLOR_2_PLAY_BLACK = "B",
	_SGF_TMP_FILE_SUFFIX = "_tmp",
	_ESTIMATED_SCORE_BLACK_WINS_LETTER = "B";

static final
char[] _POSITION_LETTERS_CHARS = _POSITION_LETTERS_STR.toCharArray ();

private static final
List <Point> _markers = Generics.newArrayList ();

static
Pattern _sgfCommentPattern;

private final
MainActivity _mainActivity;

private final
Handler _myHandler;

private final
BoardView _boardView;

private
int _blackAutoRestoredCaptures, _whiteAutoRestoredCaptures;

Gtp (
	final MainActivity pMainActivity,
	final BoardView pBoardView,
	final Handler pHandler
	)
{
	_mainActivity = pMainActivity;
	_boardView = pBoardView;
	_myHandler = pHandler;
}

private
void executeCommand (
	final int pCommand,
	final GameInfo pArgument
	)
{
	final Handler handler = _myHandler;
	handler.sendMessage (handler.obtainMessage (pCommand, pArgument));
}

private
void executeMainCommand (
	final int pCommand,
	final int pArg1,
	final int pArg2,
	final Object pObject
	)
{
	final Handler mainHandler = _mainActivity._mainHandler;
	mainHandler.sendMessage (
		mainHandler.obtainMessage (pCommand, pArg1, pArg2, pObject));
}

void drawBoard (
	final GameInfo pGameInfo,
	final boolean pInit
	)
{
	if (pInit)
	{
		executeCommand (Command.INIT_BOARD._cmd, pGameInfo);
	}
	executeCommand (Command.DRAW_BOARD._cmd, pGameInfo);
}

void newGame (
	final GameInfo pGameInfo
	)
{
	_boardView.setZoom (false);
	executeCommand (Command.NEW_GAME._cmd, pGameInfo);
}

private static
final GameInfo [] _changeGameInfos = new GameInfo [2];

void changeGame (
	final GameInfo pOldGame,
	final GameInfo pNewGame
	)
{
	final Handler handler = _myHandler;
	final GameInfo [] changeGameInfos = _changeGameInfos;
	changeGameInfos [0] = pOldGame;
	changeGameInfos [1] = pNewGame;
	handler.sendMessage (handler.obtainMessage (
		Command.CHANGE_GAME._cmd, changeGameInfos));
}

void nextMove (
	final GameInfo pGameInfo
	)
{
	_boardView.setZoom (false);
	_boardView.lockScreen (true);
	executeCommand (Command.PLAY_MOVE._cmd, pGameInfo);
}

void hideWaitProgress ()
{
	executeCommand (Command.HIDE_WAIT_PROGRESS._cmd, null);
}

void undoMove (
	final GameInfo pGameInfo
	)
{
	_boardView.setZoom (false);
	executeCommand (Command.UNDO_MOVE._cmd, pGameInfo);
}

void redoMove (
	final GameInfo pGameInfo
	)
{
	_boardView.setZoom (false);
	executeCommand (Command.REDO_MOVE._cmd, pGameInfo);
}

void saveGame (
	final GameInfo pGameInfo
	)
{
	executeCommand (Command.SAVE_GAME._cmd, pGameInfo);
}

void loadGame (
	final GameInfo pGameInfo
	)
{
	executeCommand (Command.LOAD_GAME._cmd, pGameInfo);
}

private
void showMove (
	final boolean pBlack,
	final String pMove
	)
{
	executeMainCommand (MainCommand.SHOW_MOVE._cmd, pBlack ? 1 : 0, 0, pMove);
}

private
void showCaptures (
	final int pBlack,
	final int pWhite
	)
{
	executeMainCommand (MainCommand.SHOW_CAPTURES._cmd, pBlack, pWhite, null);
}

private
void showScore (
	final int pBlackTerritory,
	final int pWhiteTerritory,
	final Object pStatus
	)
{
	executeMainCommand (MainCommand.SHOW_SCORE._cmd,
		pBlackTerritory, pWhiteTerritory, pStatus);
}

private
void showWaitProgress (
	final String pMessage
	)
{
	executeMainCommand (MainCommand.SHOW_WAIT_PROGRESS._cmd, 0, 0, pMessage);
}

private
void enablePassMenu (
	final boolean pEnable
	)
{
	executeMainCommand (MainCommand.ENABLE_PASS_MENU._cmd, 0, 0, pEnable);
}

private
void enableUndoMenu (
	final GameInfo pGameInfo
	)
{
	executeMainCommand (MainCommand.ENABLE_UNDO_MENU._cmd, 0, 0, pGameInfo);
}

static
List <Point> getMarkers ()
{
	return _markers;
}

private static
void storeMarkers ()
{
	final List <Point> markers = _markers;
	markers.clear ();
	final String board = gtpCommand (GtpCommand.SHOWBOARD, null);
	if (board == null)
	{
		return;
	}
	final int boardLength = board.length ();
	int x = 0, y = -2;
	for (int idx=0; idx < boardLength; idx++)
	{
		switch (board.charAt (idx))
		{
		case '+':
			markers.add (new Point (x, y));
		case '.':
			x += 1;
			break;
		case '\n':
			x = 0; y += 1;
			break;
		}
	}
}

static private
String gtpCommand (
	final GtpCommand pCommand,
	final String pArgument
	)
{
	final String cmd = pCommand._gtpCommand,
		command = pArgument == null ? cmd : cmd + pArgument;

	String reply = playGTP (command);

	if (reply != null)
	{
		reply = reply.replaceFirst ("= ", "").replace ("\n\n", "");
	}
	if (isEnabledFor (_LOG_TAG, Log.DEBUG))
	{
		log (_LOG_TAG, Log.DEBUG, "command: '"
			+ command + "' reply: '" + reply + "'");
	}
	if (reply == null || reply.length () == 0 || reply.charAt (0) == '?')
	{
		return "";
	}
	return reply;
}

final
void handleMessage (
	final Message pMessage
	)
{
	final Command cmd =
		_cmdMessagesMap.get (pMessage.what);
	if (cmd == null)
	{
		return;
	}
	if (cmd == Command.HIDE_WAIT_PROGRESS)
	{
		_mainActivity.hideWaitProgress ();
		return;
	}
	final GameInfo gameInfo =
		cmd == Command.CHANGE_GAME ? ((GameInfo [])pMessage.obj) [0]
		: (GameInfo)pMessage.obj;
	if (gameInfo._invalid
		&& cmd != Command.SAVE_GAME
		&& cmd != Command.LOAD_GAME)
	{
		return;
	}
	final BoardView boardView = _boardView;
	switch (cmd)
	{
	case INIT_BOARD:
		boardView.initBoard (gameInfo._boardSize);
		return;
	case UNDO_MOVE:
	case REDO_MOVE:
		final boolean isUndo = cmd == Command.UNDO_MOVE;
		int numUndos = 1;
		gameInfo._playerBlackMoves = !gameInfo._playerBlackMoves;
		boolean playerIsMachine = playerIsMachine (gameInfo);
		if (playerIsMachine)
		{
			numUndos++;
			if (gameInfo._playerBlackMoves)
			{
				gameInfo._playerBlackPassed = false;
			}
			else
			{
				gameInfo._playerWhitePassed = false;
			}
			gameInfo._playerBlackMoves = !gameInfo._playerBlackMoves;
		}
		boolean playerBlackMoves = gameInfo._playerBlackMoves;
		final List <Point> moveHistory = gameInfo._moveHistory;
		final int numHistoryStones = moveHistory.size () -1;
		final Point undoPoint;
		try
		{
			undoPoint = moveHistory.get (numHistoryStones
				- (gameInfo._moveHistoryOffset +=
					numUndos * (isUndo ? 1 : -1)));
		}
		catch (final IndexOutOfBoundsException e)
		{
			gameInfo._invalid = true;
			enableUndoMenu (null);
			return;
		}
		if (undoPoint == null)
		{
			if (playerBlackMoves)
			{
				gameInfo._playerBlackPassed = false;
			}
			else
			{
				gameInfo._playerWhitePassed = false;
			}
		}
		if (isUndo)
		{
			gtpCommand (GtpCommand.UNDO, Integer.toString (numUndos));
		}
		else
		{
			boolean playerColorBlack =
				playerIsMachine ? playerBlackMoves : !playerBlackMoves;
			final int moveHistoryOffset = gameInfo._moveHistoryOffset,
				boardSize = gameInfo._boardSize;
			for (numUndos--; numUndos >= 0; numUndos--)
			{
				final Point redoMove = moveHistory.get (numHistoryStones
					- moveHistoryOffset - numUndos);
				gtpCommand (GtpCommand.PLAY_MOVE,
					(playerColorBlack ? _BLACK : _WHITE) + " "
						+ (redoMove == null ? _PASS
							: point2Vertex (redoMove, boardSize)));
				playerColorBlack = !playerColorBlack;
			}
		}
		storeAutoSavedGame (gameInfo);
		showMove (!playerBlackMoves,
			undoPoint == null ? _mainActivity.getPassedText (false)
				: point2Vertex (undoPoint, gameInfo._boardSize));
		showMove (playerBlackMoves, "");
		drawBoard (gameInfo, false);
		enableUndoMenu (gameInfo);
		return;
	case NEW_GAME:
		if (!loadSavedGame (gameInfo, true))
		{
			gtpCommand (GtpCommand.SET_BOARDSIZE,
				String.valueOf (gameInfo._boardSize));
			storeMarkers ();
			if (gameInfo._handicap > 0)
			{
				gtpCommand (GtpCommand.SET_HANDICAP,
					String.valueOf (gameInfo._handicap));
				gameInfo._playerBlackMoves = false;
			}
			_blackAutoRestoredCaptures = _whiteAutoRestoredCaptures = 0;
			storeAutoSavePlayerStatus (gameInfo);
		}
		showCaptures (0, 0);
		if (gameInfo._sgfComment != null)
		{
			showScore (MainActivity._SHOW_MESSAGE, MainActivity._SHOW_MESSAGE,
				gameInfo._sgfComment);
		}
		else
		{
			showScore (0, 0, null);
		}
		showMove (true, ""); showMove (false, "");
		reStartGame (gameInfo, true);
		return;
	case CHANGE_GAME:
		gameInfo._invalid = true;
		final GameInfo newGame = ((GameInfo [])pMessage.obj) [1];
		newGame._moveHistory.addAll (gameInfo._moveHistory);
		newGame._moveHistoryOffset = gameInfo._moveHistoryOffset;
		newGame._playerBlackMoves = gameInfo._playerBlackMoves;
		newGame._playerBlackPassed = gameInfo._playerBlackPassed;
		newGame._playerWhitePassed = gameInfo._playerWhitePassed;
		newGame._sgfComment = gameInfo._sgfComment;
		newGame._sgfFileName = gameInfo._sgfFileName;
		storeAutoSavePlayerStatus (newGame);
		showMove (newGame._playerBlackMoves, "");
		reStartGame (newGame, false);
		return;
	case DRAW_BOARD:
		playerBlackMoves = gameInfo._playerBlackMoves;
		boardView.drawBoard (
			getStones (true, gameInfo), getStones (false, gameInfo),
			gameInfo._moveHistory, gameInfo._moveHistoryOffset,
			!playerBlackMoves);
		showCaptures (getCaptures (true), getCaptures (false));
		if (!playerIsMachine (gameInfo))
		{
			_boardView.setLegalMoves (verteces2Points (
				gtpCommand (GtpCommand.LIST_LEGAL,
					playerBlackMoves ? _BLACK : _WHITE), gameInfo));
		}
		return;
	case PLAY_MOVE:
		playerBlackMoves = gameInfo._playerBlackMoves;
		playerIsMachine = playerIsMachine (gameInfo);
		if (playerIsMachine)
		{
			enablePassMenu (false);
			enableUndoMenu (null);
			showMove (playerBlackMoves, null);
		}
		final String color = playerBlackMoves ? _BLACK : _WHITE;
		final Point lastMove = gameInfo.getLastMove ();
		final String move = gtpCommand (
			playerIsMachine ? GtpCommand.GEN_MOVE : GtpCommand.PLAY_MOVE,
			color +
				(playerIsMachine ? ""
					: (" " + (lastMove == null ? _PASS
						: point2Vertex (lastMove, gameInfo._boardSize)))));
		if (gameInfo._invalid)
		{
			return;
		}
		final boolean resigned = _RESIGN.equals (move),
			passed = _PASS.equals (move) || resigned;
		if (playerIsMachine)
		{
			if (playerBlackMoves)
			{
				gameInfo._playerBlackPassed = passed;
			}
			else
			{
				gameInfo._playerWhitePassed = passed;
			}
			gameInfo.resetMoveHistory ();
			gameInfo._moveHistory.add (
				passed ? null : vertex2Point (move, gameInfo));
			showMove (playerBlackMoves,
				passed ? _mainActivity.getPassedText (resigned) : move);
		}
		gameInfo._playerBlackMoves = !playerBlackMoves;
		storeAutoSavedGame (gameInfo);
		final boolean nextPlayerIsMachine = playerIsMachine (gameInfo);
		if ((gameInfo._playerBlackPassed && gameInfo._playerWhitePassed)
			|| resigned)
		{
			if (!resigned)
			{
				score (gameInfo);
			}
			else
			{
				enablePassMenu (false);
				enableUndoMenu (null);
				gameInfo._invalid = true;
			}
			return;
		}
		if (passed && playerIsMachine && !nextPlayerIsMachine)
		{
			_mainActivity.showPassMessage (playerBlackMoves, resigned);
		}
		drawBoard (gameInfo, false);
		if (nextPlayerIsMachine)
		{
			nextMove (gameInfo);
		}
		else
		{
			showMove (!playerBlackMoves, "");
			enablePassMenu (true);
			enableUndoMenu (gameInfo);
			boardView.lockScreen (false);
		}
		return;
	case SAVE_GAME:
		final String sgfFileName = gameInfo._sgfFileName;
		gtpCommand (GtpCommand.SAVE_SGF, sgfFileName);
		storeAutoSavedGame (gameInfo);
		MainActivity mainActivity = _mainActivity;
		mainActivity.showMessage (mainActivity._resources.getString (
			R.string.gameSavedInFileMessage, sgfFileName));
		return;
	case LOAD_GAME:
		if (!loadSavedGame (gameInfo, false))
		{
			mainActivity = _mainActivity;
			mainActivity.showMessage (mainActivity._resources.getString (
				R.string.loadGameFailedAlertMessage));
			return;
		}
		final String comment = readSgfComment (gameInfo);
		if (comment != null)
		{
			showScore (MainActivity._SHOW_MESSAGE, MainActivity._SHOW_MESSAGE,
				comment);
		}
		else
		{
			showScore (0, 0, null);
		}
		storeAutoSavedGame (gameInfo);
		showCaptures (0, 0);
		showMove (true, ""); showMove (false, "");
		reStartGame (gameInfo, true);
	}
}

private
void reStartGame (
	final GameInfo pGameInfo,
	final boolean pInit
	)
{
	gtpCommand (GtpCommand.SET_LEVEL, String.valueOf (pGameInfo._level));
	gtpCommand (GtpCommand.SET_KOMI, pGameInfo._komi);
	final int chineseRules = pGameInfo._chineseRules ? 1 : 0;
	MainActivity.setRules (chineseRules);
	if (isEnabledFor (_LOG_TAG, Log.DEBUG))
	{
		log (_LOG_TAG, Log.DEBUG, "command: 'chineseRules': " + chineseRules);
	}
	drawBoard (pGameInfo, pInit);
	_boardView.lockScreen (false);
	if (playerIsMachine (pGameInfo))
	{
		nextMove (pGameInfo);
	}
	else
	{
		enablePassMenu (true);
		enableUndoMenu (pGameInfo);
	}
}

private
void score (
	final GameInfo pGameInfo
	)
{
	pGameInfo._invalid = true;
	enablePassMenu (false);
	enableUndoMenu (null);
	final Resources resources = _mainActivity._resources;
	if (pGameInfo._playerBlackHuman && pGameInfo._playerWhiteHuman)
	{
		showWaitProgress (resources.
			getString (R.string.waitProgressEstimatingScoreMessage));
		final Matcher matcher =
			Pattern.compile (_ESTIMIATED_SCORE_PATTERN).matcher (
				gtpCommand (GtpCommand.ESTIMATE_SCORE, null));
		if (matcher.find ())
		{
			final boolean blackWins =
				_ESTIMATED_SCORE_BLACK_WINS_LETTER.equals (matcher.group (1));
			showScore (blackWins ? MainActivity._SHOW_ESTIMATED_SCORE : 0,
				blackWins ? 0 : MainActivity._SHOW_ESTIMATED_SCORE,
				matcher.group (2));
		}
		return;
	}

	showWaitProgress (resources.
		getString (R.string.waitProgressFinalScoreMessage));
	final BoardView boardView = _boardView;
	final List <Point> blackStones = getStones (true, pGameInfo),
		whiteStones = getStones (false, pGameInfo),
		deadStones = verteces2Points (
			gtpCommand (GtpCommand.LIST_FINAL_STATUS, _DEAD), pGameInfo),
		blackDeadStones = Generics.newArrayList (),
		whiteDeadStones = Generics.newArrayList ();
	for (final Point dead : deadStones)
	{
		(blackStones.contains (dead) ? blackDeadStones : whiteDeadStones).
			add (dead);
	}
	removeDeadStones (blackDeadStones, blackStones);
	removeDeadStones (whiteDeadStones, whiteStones);
	boardView.drawBoard (blackStones, whiteStones, null, 0, false);
	boardView.drawDeadStones (blackDeadStones, whiteDeadStones);
	final List <Point>
		blackTerritory = verteces2Points (
			gtpCommand (GtpCommand.LIST_FINAL_STATUS, _BLACK_TERRITORY),
			pGameInfo),
		whiteTerritory = verteces2Points (
			gtpCommand (GtpCommand.LIST_FINAL_STATUS, _WHITE_TERRITORY),
			pGameInfo);
	boardView.drawTerritory (blackTerritory, whiteTerritory, false);
	boardView.drawTerritory (whiteDeadStones, blackDeadStones, true);
	boardView.drawBoard2Surface ();
	final int numBlackDeadStones = blackDeadStones.size (),
		numWhiteDeadStones = whiteDeadStones.size ();
	final boolean chineseRules = pGameInfo._chineseRules;
	showCaptures (
		(chineseRules ? blackStones.size () : getCaptures (true))
			+ numWhiteDeadStones,
		(chineseRules ? whiteStones.size () : getCaptures (false))
			+ numBlackDeadStones);
	showScore (blackTerritory.size () + numWhiteDeadStones,
		whiteTerritory.size () + numBlackDeadStones, pGameInfo);
}

private static
void removeDeadStones (
	final List <Point> pDeadStones,
	final List <Point> pAliveStones
	)
{
	for (final Point dead : pDeadStones)
	{
		pAliveStones.remove (dead);
	}
}

void deleteAutoSaveFile ()
{
	final String autoSavePathFileName = _mainActivity._autoSaveGamePathFileName;
	final File autoSaveFile = new File (autoSavePathFileName);
	if (autoSaveFile.exists ())
	{
		synchronized (autoSavePathFileName)
		{
			autoSaveFile.delete ();
		}
	}
}

private
boolean loadSavedGame (
	final GameInfo pGameInfo,
	final boolean pAutoSaved
	)
{
	final String savePathFileName = pAutoSaved ?
		_mainActivity._autoSaveGamePathFileName :
		pGameInfo._sgfFileName;
	final File saveFile = new File (savePathFileName);
	if (!saveFile.exists ())
	{
		return false;
	}
	final String color = gtpCommand (GtpCommand.LOAD_SGF, savePathFileName);
	if ("".equals (color))
	{
		return false;
	}
	final String boardSize = gtpCommand (GtpCommand.GET_BOARDSIZE, null);
	pGameInfo._boardSize = Integer.parseInt (boardSize);
	gtpCommand (GtpCommand.SET_BOARDSIZE, boardSize);
	storeMarkers ();
	gtpCommand (GtpCommand.LOAD_SGF, savePathFileName);
	if (!pAutoSaved)
	{
		pGameInfo._playerBlackMoves = _BLACK.equals (color);
		return true;
	}

	final MainActivity mainActivity = _mainActivity;
	final SharedPreferences preferences = mainActivity._sharedPreferences;
	final boolean playerBlackMoves =
		pGameInfo._playerBlackMoves = preferences.getBoolean (
			mainActivity._preferencesPlayerBlackMovesAutoSaveKey,
			pGameInfo._playerBlackMoves);
	pGameInfo._playerBlackHuman = preferences.getBoolean (
		mainActivity._preferencesPlayerBlackHumanAutoSaveKey,
		pGameInfo._playerBlackHuman);
	pGameInfo._playerWhiteHuman = preferences.getBoolean (
		mainActivity._preferencesPlayerWhiteHumanAutoSaveKey,
		pGameInfo._playerWhiteHuman);
	pGameInfo._komi = preferences.getString (
		mainActivity._preferencesKomiAutoSaveKey,
		pGameInfo._komi);
	pGameInfo._chineseRules = preferences.getBoolean (
		mainActivity._preferencesChineseRulesAutoSaveKey,
		pGameInfo._chineseRules);
	pGameInfo._level = preferences.getInt (
		mainActivity._preferencesLevelAutoSaveKey,
		pGameInfo._level);
	pGameInfo._sgfComment = preferences.getString (
		mainActivity._preferencesSgfCommentAutoSaveKey, null);
	pGameInfo._sgfFileName = preferences.getString (
		mainActivity._preferencesSgfFileNameAutoSaveKey, null);
	String lastMove = preferences.getString (
		mainActivity._preferencesLastMoveAutoSaveKey, null);
	pGameInfo._moveHistory.add (vertex2Point (lastMove, pGameInfo));
	if (lastMove == null)
	{
		if (playerBlackMoves)
		{
			pGameInfo._playerWhitePassed = true;
		}
		else
		{
			pGameInfo._playerBlackPassed = true;
		}
	}
	_blackAutoRestoredCaptures = preferences.getInt (
		mainActivity._preferencesBlackCapturesAutoSaveKey, 0);
	_whiteAutoRestoredCaptures = preferences.getInt (
		mainActivity._preferencesWhiteCapturesAutoSaveKey, 0);
	lastMove = lastMove == null ? mainActivity.getPassedText (false) : lastMove;
	showMove (true, playerBlackMoves ? "" : lastMove);
	showMove (false, !playerBlackMoves ? "" : lastMove);
	return true;
}

private
void storeAutoSavedGame (
	final GameInfo pGameInfo
	)
{
	final String autoSavePathFileName = _mainActivity._autoSaveGamePathFileName;
	synchronized (autoSavePathFileName)
	{
		gtpCommand (GtpCommand.SAVE_SGF, autoSavePathFileName);
	}
	final MainActivity mainActivity = _mainActivity;
	final SharedPreferences.Editor preferences =
		mainActivity._sharedPreferences.edit ();
	preferences.putBoolean (
		mainActivity._preferencesPlayerBlackMovesAutoSaveKey,
		pGameInfo._playerBlackMoves);
	preferences.putString (mainActivity._preferencesLastMoveAutoSaveKey,
		point2Vertex (pGameInfo.getLastMove (), pGameInfo._boardSize));
	preferences.putInt (mainActivity._preferencesBlackCapturesAutoSaveKey,
		getCaptures (true));
	preferences.putInt (mainActivity._preferencesWhiteCapturesAutoSaveKey,
		getCaptures (false));
	preferences.putString (mainActivity._preferencesSgfCommentAutoSaveKey,
		pGameInfo._sgfComment);
	preferences.putString (mainActivity._preferencesSgfFileNameAutoSaveKey,
		pGameInfo._sgfFileName);
	preferences.commit ();
}

private
void storeAutoSavePlayerStatus (
	final GameInfo pGameInfo
	)
{
	final MainActivity mainActivity = _mainActivity;
	final SharedPreferences.Editor preferences =
		mainActivity._sharedPreferences.edit ();
	preferences.putBoolean (
		mainActivity._preferencesPlayerBlackHumanAutoSaveKey,
		pGameInfo._playerBlackHuman);
	preferences.putBoolean (
		mainActivity._preferencesPlayerWhiteHumanAutoSaveKey,
		pGameInfo._playerWhiteHuman);
	preferences.putString (
		mainActivity._preferencesKomiAutoSaveKey, pGameInfo._komi);
	preferences.putBoolean (
		mainActivity._preferencesChineseRulesAutoSaveKey,
		pGameInfo._chineseRules);
	preferences.putInt (
		mainActivity._preferencesLevelAutoSaveKey, pGameInfo._level);
	preferences.commit ();
}

private static
String readSgfComment (
	final GameInfo pGameInfo
	)
{
	BufferedReader reader = null;
	try
	{
		Pattern pattern = _sgfCommentPattern;
		if (pattern == null)
		{
			pattern = _sgfCommentPattern =
				Pattern.compile (_SGF_COMMENT_PATTERN_STRING
					+ _SGF_VALUE_PATTERN_STRING);
		}
		final String fileNamePath = pGameInfo._sgfFileName;
		reader = new BufferedReader (new FileReader (new File (fileNamePath)));
		String line;
		while ((line = reader.readLine ()) != null)
		{
			final Matcher matcher = pattern.matcher (line);
			if (matcher.find ())
			{
				final String match = matcher.group (1);
					return pGameInfo._sgfComment = match;
				}
		}
	}
	catch (final Exception e) {}
	finally
	{
		if (reader != null)
		{
			try
			{
				reader.close ();
			}
			catch (final Exception e) {}
		}
	}
	return null;
}

private static
List <Point> getStones (
	final boolean pBlack,
	final GameInfo pGameInfo
	)
{
	return verteces2Points (
		gtpCommand (
			GtpCommand.LIST_STONES, pBlack ? _BLACK : _WHITE), pGameInfo);
}

private
int getCaptures (
	final boolean pBlack
	)
{
	return Integer.parseInt (
		gtpCommand (GtpCommand.GET_CAPTURES, pBlack ? _BLACK : _WHITE)) +
			(pBlack ? _blackAutoRestoredCaptures : _whiteAutoRestoredCaptures);
}

private static
Point vertex2Point (
	final String pVertex,
	final GameInfo pGameInfo
	)
{
	if (pVertex == null)
	{
		return null;
	}
	return new Point (
		_POSITION_LETTERS_STR.indexOf (pVertex.charAt (0)),
		pGameInfo._boardSize - Integer.parseInt (pVertex.substring (1)));
}

private static
List <Point> verteces2Points (
	final String pVerteces,
	final GameInfo pGameInfo
	)
{
	final List <Point> points = Generics.newArrayList ();
	if (pVerteces == null)
	{
		return points;
	}
	final StringTokenizer tokenizer = new StringTokenizer (pVerteces, " \n");
	while (tokenizer.hasMoreTokens ())
	{
		points.add (new Point (
			vertex2Point (tokenizer.nextToken (), pGameInfo)));
	}
	return points;
}

static
String point2Vertex (
	final Point pPoint,
	final int pBoardSize
	)
{
	if (pPoint == null)
	{
		return null;
	}
	return _POSITION_LETTERS_CHARS [pPoint.x]
		+ String.valueOf (pBoardSize - pPoint.y);
}

static
boolean playerIsMachine (
	final GameInfo pGameInfo
	)
{
	if (pGameInfo == null)
	{
		return false;
	}
	final boolean playerBlackMoves = pGameInfo._playerBlackMoves;
	return (playerBlackMoves && !pGameInfo._playerBlackHuman)
		|| (!playerBlackMoves && !pGameInfo._playerWhiteHuman);
}
}