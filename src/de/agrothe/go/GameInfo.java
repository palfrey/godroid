package de.agrothe.go;

import java.util.LinkedList;
import java.util.List;

import android.graphics.Point;
import de.agrothe.util.Generics;

public
class GameInfo
{
	int _boardSize = 9;

	int _level = 1;

	int _handicap = 0;

	String _komi = "5.5";

	boolean _chineseRules = false;

	String _sgfFileName;

	String _sgfComment;

	boolean
		_playerBlackHuman = true,
		_playerWhiteHuman = false;

	boolean
		_playerBlackMoves = true,
		_playerBlackPassed = false,
		_playerWhitePassed = false;

	final LinkedList <Point> _moveHistory = Generics.newLinkedList ();

	int _moveHistoryOffset = 0;

	boolean _invalid = false;

	Point getLastMove ()
	{
		final List <Point> moveHistory = _moveHistory;
		if (moveHistory.size () == 0)
		{
			return null;
		}
		return moveHistory.get (moveHistory.size () - _moveHistoryOffset -1);
	}

	void resetMoveHistory ()
	{
		final LinkedList <Point> moveHistory = _moveHistory;
		for (int historySize = _moveHistoryOffset; historySize > 0;
			historySize--)
		{
			moveHistory.removeLast ();
		}
		_moveHistoryOffset = 0;
	}
}