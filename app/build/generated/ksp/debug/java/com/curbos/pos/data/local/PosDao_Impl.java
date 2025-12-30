package com.curbos.pos.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.curbos.pos.data.model.MenuItem;
import com.curbos.pos.data.model.ModifierOption;
import com.curbos.pos.data.model.OfflineTransaction;
import com.curbos.pos.data.model.Transaction;
import com.curbos.pos.data.model.TransactionItem;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PosDao_Impl implements PosDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ModifierOption> __insertionAdapterOfModifierOption;

  private final EntityInsertionAdapter<MenuItem> __insertionAdapterOfMenuItem;

  private final EntityInsertionAdapter<Transaction> __insertionAdapterOfTransaction;

  private final Converters __converters = new Converters();

  private final EntityInsertionAdapter<OfflineTransaction> __insertionAdapterOfOfflineTransaction;

  private final EntityDeletionOrUpdateAdapter<ModifierOption> __deletionAdapterOfModifierOption;

  private final EntityDeletionOrUpdateAdapter<MenuItem> __deletionAdapterOfMenuItem;

  private final EntityDeletionOrUpdateAdapter<OfflineTransaction> __deletionAdapterOfOfflineTransaction;

  private final EntityDeletionOrUpdateAdapter<ModifierOption> __updateAdapterOfModifierOption;

  private final EntityDeletionOrUpdateAdapter<MenuItem> __updateAdapterOfMenuItem;

  private final EntityDeletionOrUpdateAdapter<Transaction> __updateAdapterOfTransaction;

  private final SharedSQLiteStatement __preparedStmtOfClearMenuItems;

  public PosDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfModifierOption = new EntityInsertionAdapter<ModifierOption>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `modifier_options` (`id`,`name`,`priceDelta`,`type`,`category`,`isAvailable`,`updatedAt`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ModifierOption entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindDouble(3, entity.getPriceDelta());
        statement.bindString(4, entity.getType());
        if (entity.getCategory() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getCategory());
        }
        final int _tmp = entity.isAvailable() ? 1 : 0;
        statement.bindLong(6, _tmp);
        if (entity.getUpdatedAt() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getUpdatedAt());
        }
      }
    };
    this.__insertionAdapterOfMenuItem = new EntityInsertionAdapter<MenuItem>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `menu_items` (`id`,`name`,`category`,`price`,`imageUrl`,`taxRate`,`isAvailable`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MenuItem entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getCategory());
        statement.bindDouble(4, entity.getPrice());
        if (entity.getImageUrl() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getImageUrl());
        }
        statement.bindDouble(6, entity.getTaxRate());
        final int _tmp = entity.isAvailable() ? 1 : 0;
        statement.bindLong(7, _tmp);
        if (entity.getUpdatedAt() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getUpdatedAt());
        }
      }
    };
    this.__insertionAdapterOfTransaction = new EntityInsertionAdapter<Transaction>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `transactions` (`id`,`timestamp`,`totalAmount`,`taxAmount`,`items`,`status`,`paymentMethod`,`fulfillmentStatus`,`orderNumber`,`customerName`,`squareTransactionId`,`isSynced`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Transaction entity) {
        statement.bindString(1, entity.getId());
        statement.bindLong(2, entity.getTimestamp());
        statement.bindDouble(3, entity.getTotalAmount());
        statement.bindDouble(4, entity.getTaxAmount());
        final String _tmp = __converters.fromTransactionItemList(entity.getItems());
        statement.bindString(5, _tmp);
        statement.bindString(6, entity.getStatus());
        statement.bindString(7, entity.getPaymentMethod());
        statement.bindString(8, entity.getFulfillmentStatus());
        if (entity.getOrderNumber() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getOrderNumber());
        }
        if (entity.getCustomerName() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getCustomerName());
        }
        if (entity.getSquareTransactionId() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getSquareTransactionId());
        }
        final int _tmp_1 = entity.isSynced() ? 1 : 0;
        statement.bindLong(12, _tmp_1);
      }
    };
    this.__insertionAdapterOfOfflineTransaction = new EntityInsertionAdapter<OfflineTransaction>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `offline_transactions` (`id`,`transactionJson`,`timestamp`) VALUES (nullif(?, 0),?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final OfflineTransaction entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTransactionJson());
        statement.bindLong(3, entity.getTimestamp());
      }
    };
    this.__deletionAdapterOfModifierOption = new EntityDeletionOrUpdateAdapter<ModifierOption>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `modifier_options` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ModifierOption entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__deletionAdapterOfMenuItem = new EntityDeletionOrUpdateAdapter<MenuItem>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `menu_items` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MenuItem entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__deletionAdapterOfOfflineTransaction = new EntityDeletionOrUpdateAdapter<OfflineTransaction>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `offline_transactions` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final OfflineTransaction entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfModifierOption = new EntityDeletionOrUpdateAdapter<ModifierOption>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `modifier_options` SET `id` = ?,`name` = ?,`priceDelta` = ?,`type` = ?,`category` = ?,`isAvailable` = ?,`updatedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ModifierOption entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindDouble(3, entity.getPriceDelta());
        statement.bindString(4, entity.getType());
        if (entity.getCategory() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getCategory());
        }
        final int _tmp = entity.isAvailable() ? 1 : 0;
        statement.bindLong(6, _tmp);
        if (entity.getUpdatedAt() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getUpdatedAt());
        }
        statement.bindString(8, entity.getId());
      }
    };
    this.__updateAdapterOfMenuItem = new EntityDeletionOrUpdateAdapter<MenuItem>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `menu_items` SET `id` = ?,`name` = ?,`category` = ?,`price` = ?,`imageUrl` = ?,`taxRate` = ?,`isAvailable` = ?,`updatedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MenuItem entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getCategory());
        statement.bindDouble(4, entity.getPrice());
        if (entity.getImageUrl() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getImageUrl());
        }
        statement.bindDouble(6, entity.getTaxRate());
        final int _tmp = entity.isAvailable() ? 1 : 0;
        statement.bindLong(7, _tmp);
        if (entity.getUpdatedAt() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getUpdatedAt());
        }
        statement.bindString(9, entity.getId());
      }
    };
    this.__updateAdapterOfTransaction = new EntityDeletionOrUpdateAdapter<Transaction>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `transactions` SET `id` = ?,`timestamp` = ?,`totalAmount` = ?,`taxAmount` = ?,`items` = ?,`status` = ?,`paymentMethod` = ?,`fulfillmentStatus` = ?,`orderNumber` = ?,`customerName` = ?,`squareTransactionId` = ?,`isSynced` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Transaction entity) {
        statement.bindString(1, entity.getId());
        statement.bindLong(2, entity.getTimestamp());
        statement.bindDouble(3, entity.getTotalAmount());
        statement.bindDouble(4, entity.getTaxAmount());
        final String _tmp = __converters.fromTransactionItemList(entity.getItems());
        statement.bindString(5, _tmp);
        statement.bindString(6, entity.getStatus());
        statement.bindString(7, entity.getPaymentMethod());
        statement.bindString(8, entity.getFulfillmentStatus());
        if (entity.getOrderNumber() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getOrderNumber());
        }
        if (entity.getCustomerName() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getCustomerName());
        }
        if (entity.getSquareTransactionId() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getSquareTransactionId());
        }
        final int _tmp_1 = entity.isSynced() ? 1 : 0;
        statement.bindLong(12, _tmp_1);
        statement.bindString(13, entity.getId());
      }
    };
    this.__preparedStmtOfClearMenuItems = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM menu_items";
        return _query;
      }
    };
  }

  @Override
  public Object insertModifier(final ModifierOption modifier,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfModifierOption.insert(modifier);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertMenuItems(final List<MenuItem> items,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMenuItem.insert(items);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertMenuItem(final MenuItem item, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMenuItem.insert(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertTransaction(final Transaction transaction,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTransaction.insert(transaction);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertOfflineTransaction(final OfflineTransaction transaction,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfOfflineTransaction.insert(transaction);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteModifier(final ModifierOption modifier,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfModifierOption.handle(modifier);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteMenuItem(final MenuItem item, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfMenuItem.handle(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOfflineTransaction(final OfflineTransaction transaction,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfOfflineTransaction.handle(transaction);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateModifier(final ModifierOption modifier,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfModifierOption.handle(modifier);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateMenuItem(final MenuItem item, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfMenuItem.handle(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateTransaction(final Transaction transaction,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfTransaction.handle(transaction);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearMenuItems(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearMenuItems.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearMenuItems.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MenuItem>> getAllMenuItems() {
    final String _sql = "SELECT * FROM menu_items";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"menu_items"}, new Callable<List<MenuItem>>() {
      @Override
      @NonNull
      public List<MenuItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "price");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfTaxRate = CursorUtil.getColumnIndexOrThrow(_cursor, "taxRate");
          final int _cursorIndexOfIsAvailable = CursorUtil.getColumnIndexOrThrow(_cursor, "isAvailable");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<MenuItem> _result = new ArrayList<MenuItem>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MenuItem _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final double _tmpPrice;
            _tmpPrice = _cursor.getDouble(_cursorIndexOfPrice);
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final double _tmpTaxRate;
            _tmpTaxRate = _cursor.getDouble(_cursorIndexOfTaxRate);
            final boolean _tmpIsAvailable;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsAvailable);
            _tmpIsAvailable = _tmp != 0;
            final String _tmpUpdatedAt;
            if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
              _tmpUpdatedAt = null;
            } else {
              _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            }
            _item = new MenuItem(_tmpId,_tmpName,_tmpCategory,_tmpPrice,_tmpImageUrl,_tmpTaxRate,_tmpIsAvailable,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<MenuItem>> getMenuItemsByCategory(final String category) {
    final String _sql = "SELECT * FROM menu_items WHERE category = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, category);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"menu_items"}, new Callable<List<MenuItem>>() {
      @Override
      @NonNull
      public List<MenuItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "price");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfTaxRate = CursorUtil.getColumnIndexOrThrow(_cursor, "taxRate");
          final int _cursorIndexOfIsAvailable = CursorUtil.getColumnIndexOrThrow(_cursor, "isAvailable");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<MenuItem> _result = new ArrayList<MenuItem>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MenuItem _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final double _tmpPrice;
            _tmpPrice = _cursor.getDouble(_cursorIndexOfPrice);
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final double _tmpTaxRate;
            _tmpTaxRate = _cursor.getDouble(_cursorIndexOfTaxRate);
            final boolean _tmpIsAvailable;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsAvailable);
            _tmpIsAvailable = _tmp != 0;
            final String _tmpUpdatedAt;
            if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
              _tmpUpdatedAt = null;
            } else {
              _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            }
            _item = new MenuItem(_tmpId,_tmpName,_tmpCategory,_tmpPrice,_tmpImageUrl,_tmpTaxRate,_tmpIsAvailable,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ModifierOption>> getAllModifiers() {
    final String _sql = "SELECT * FROM modifier_options";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"modifier_options"}, new Callable<List<ModifierOption>>() {
      @Override
      @NonNull
      public List<ModifierOption> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPriceDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "priceDelta");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfIsAvailable = CursorUtil.getColumnIndexOrThrow(_cursor, "isAvailable");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<ModifierOption> _result = new ArrayList<ModifierOption>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ModifierOption _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final double _tmpPriceDelta;
            _tmpPriceDelta = _cursor.getDouble(_cursorIndexOfPriceDelta);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final boolean _tmpIsAvailable;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsAvailable);
            _tmpIsAvailable = _tmp != 0;
            final String _tmpUpdatedAt;
            if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
              _tmpUpdatedAt = null;
            } else {
              _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            }
            _item = new ModifierOption(_tmpId,_tmpName,_tmpPriceDelta,_tmpType,_tmpCategory,_tmpIsAvailable,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getLatestMenuUpdate(final Continuation<? super String> $completion) {
    final String _sql = "SELECT MAX(updatedAt) FROM menu_items";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<String>() {
      @Override
      @Nullable
      public String call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final String _result;
          if (_cursor.moveToFirst()) {
            final String _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(0);
            }
            _result = _tmp;
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getLatestModifierUpdate(final Continuation<? super String> $completion) {
    final String _sql = "SELECT MAX(updatedAt) FROM modifier_options";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<String>() {
      @Override
      @Nullable
      public String call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final String _result;
          if (_cursor.moveToFirst()) {
            final String _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(0);
            }
            _result = _tmp;
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Transaction>> getAllTransactions() {
    final String _sql = "SELECT * FROM transactions ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<Transaction>>() {
      @Override
      @NonNull
      public List<Transaction> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfTotalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalAmount");
          final int _cursorIndexOfTaxAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "taxAmount");
          final int _cursorIndexOfItems = CursorUtil.getColumnIndexOrThrow(_cursor, "items");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfPaymentMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "paymentMethod");
          final int _cursorIndexOfFulfillmentStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "fulfillmentStatus");
          final int _cursorIndexOfOrderNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "orderNumber");
          final int _cursorIndexOfCustomerName = CursorUtil.getColumnIndexOrThrow(_cursor, "customerName");
          final int _cursorIndexOfSquareTransactionId = CursorUtil.getColumnIndexOrThrow(_cursor, "squareTransactionId");
          final int _cursorIndexOfIsSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "isSynced");
          final List<Transaction> _result = new ArrayList<Transaction>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Transaction _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final double _tmpTotalAmount;
            _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
            final double _tmpTaxAmount;
            _tmpTaxAmount = _cursor.getDouble(_cursorIndexOfTaxAmount);
            final List<TransactionItem> _tmpItems;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfItems);
            _tmpItems = __converters.toTransactionItemList(_tmp);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpPaymentMethod;
            _tmpPaymentMethod = _cursor.getString(_cursorIndexOfPaymentMethod);
            final String _tmpFulfillmentStatus;
            _tmpFulfillmentStatus = _cursor.getString(_cursorIndexOfFulfillmentStatus);
            final Integer _tmpOrderNumber;
            if (_cursor.isNull(_cursorIndexOfOrderNumber)) {
              _tmpOrderNumber = null;
            } else {
              _tmpOrderNumber = _cursor.getInt(_cursorIndexOfOrderNumber);
            }
            final String _tmpCustomerName;
            if (_cursor.isNull(_cursorIndexOfCustomerName)) {
              _tmpCustomerName = null;
            } else {
              _tmpCustomerName = _cursor.getString(_cursorIndexOfCustomerName);
            }
            final String _tmpSquareTransactionId;
            if (_cursor.isNull(_cursorIndexOfSquareTransactionId)) {
              _tmpSquareTransactionId = null;
            } else {
              _tmpSquareTransactionId = _cursor.getString(_cursorIndexOfSquareTransactionId);
            }
            final boolean _tmpIsSynced;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsSynced);
            _tmpIsSynced = _tmp_1 != 0;
            _item = new Transaction(_tmpId,_tmpTimestamp,_tmpTotalAmount,_tmpTaxAmount,_tmpItems,_tmpStatus,_tmpPaymentMethod,_tmpFulfillmentStatus,_tmpOrderNumber,_tmpCustomerName,_tmpSquareTransactionId,_tmpIsSynced);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getUnsyncedTransactions(final Continuation<? super List<Transaction>> $completion) {
    final String _sql = "SELECT * FROM transactions WHERE isSynced = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Transaction>>() {
      @Override
      @NonNull
      public List<Transaction> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfTotalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalAmount");
          final int _cursorIndexOfTaxAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "taxAmount");
          final int _cursorIndexOfItems = CursorUtil.getColumnIndexOrThrow(_cursor, "items");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfPaymentMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "paymentMethod");
          final int _cursorIndexOfFulfillmentStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "fulfillmentStatus");
          final int _cursorIndexOfOrderNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "orderNumber");
          final int _cursorIndexOfCustomerName = CursorUtil.getColumnIndexOrThrow(_cursor, "customerName");
          final int _cursorIndexOfSquareTransactionId = CursorUtil.getColumnIndexOrThrow(_cursor, "squareTransactionId");
          final int _cursorIndexOfIsSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "isSynced");
          final List<Transaction> _result = new ArrayList<Transaction>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Transaction _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final double _tmpTotalAmount;
            _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
            final double _tmpTaxAmount;
            _tmpTaxAmount = _cursor.getDouble(_cursorIndexOfTaxAmount);
            final List<TransactionItem> _tmpItems;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfItems);
            _tmpItems = __converters.toTransactionItemList(_tmp);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpPaymentMethod;
            _tmpPaymentMethod = _cursor.getString(_cursorIndexOfPaymentMethod);
            final String _tmpFulfillmentStatus;
            _tmpFulfillmentStatus = _cursor.getString(_cursorIndexOfFulfillmentStatus);
            final Integer _tmpOrderNumber;
            if (_cursor.isNull(_cursorIndexOfOrderNumber)) {
              _tmpOrderNumber = null;
            } else {
              _tmpOrderNumber = _cursor.getInt(_cursorIndexOfOrderNumber);
            }
            final String _tmpCustomerName;
            if (_cursor.isNull(_cursorIndexOfCustomerName)) {
              _tmpCustomerName = null;
            } else {
              _tmpCustomerName = _cursor.getString(_cursorIndexOfCustomerName);
            }
            final String _tmpSquareTransactionId;
            if (_cursor.isNull(_cursorIndexOfSquareTransactionId)) {
              _tmpSquareTransactionId = null;
            } else {
              _tmpSquareTransactionId = _cursor.getString(_cursorIndexOfSquareTransactionId);
            }
            final boolean _tmpIsSynced;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsSynced);
            _tmpIsSynced = _tmp_1 != 0;
            _item = new Transaction(_tmpId,_tmpTimestamp,_tmpTotalAmount,_tmpTaxAmount,_tmpItems,_tmpStatus,_tmpPaymentMethod,_tmpFulfillmentStatus,_tmpOrderNumber,_tmpCustomerName,_tmpSquareTransactionId,_tmpIsSynced);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<Integer> getUnsyncedCount() {
    final String _sql = "SELECT count(*) FROM offline_transactions";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"offline_transactions"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getTransactionsForDay(final long startOfDay, final long endOfDay,
      final Continuation<? super List<Transaction>> $completion) {
    final String _sql = "SELECT * FROM transactions WHERE timestamp >= ? AND timestamp <= ? AND status = 'COMPLETED'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startOfDay);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endOfDay);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Transaction>>() {
      @Override
      @NonNull
      public List<Transaction> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfTotalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalAmount");
          final int _cursorIndexOfTaxAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "taxAmount");
          final int _cursorIndexOfItems = CursorUtil.getColumnIndexOrThrow(_cursor, "items");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfPaymentMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "paymentMethod");
          final int _cursorIndexOfFulfillmentStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "fulfillmentStatus");
          final int _cursorIndexOfOrderNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "orderNumber");
          final int _cursorIndexOfCustomerName = CursorUtil.getColumnIndexOrThrow(_cursor, "customerName");
          final int _cursorIndexOfSquareTransactionId = CursorUtil.getColumnIndexOrThrow(_cursor, "squareTransactionId");
          final int _cursorIndexOfIsSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "isSynced");
          final List<Transaction> _result = new ArrayList<Transaction>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Transaction _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final double _tmpTotalAmount;
            _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
            final double _tmpTaxAmount;
            _tmpTaxAmount = _cursor.getDouble(_cursorIndexOfTaxAmount);
            final List<TransactionItem> _tmpItems;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfItems);
            _tmpItems = __converters.toTransactionItemList(_tmp);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpPaymentMethod;
            _tmpPaymentMethod = _cursor.getString(_cursorIndexOfPaymentMethod);
            final String _tmpFulfillmentStatus;
            _tmpFulfillmentStatus = _cursor.getString(_cursorIndexOfFulfillmentStatus);
            final Integer _tmpOrderNumber;
            if (_cursor.isNull(_cursorIndexOfOrderNumber)) {
              _tmpOrderNumber = null;
            } else {
              _tmpOrderNumber = _cursor.getInt(_cursorIndexOfOrderNumber);
            }
            final String _tmpCustomerName;
            if (_cursor.isNull(_cursorIndexOfCustomerName)) {
              _tmpCustomerName = null;
            } else {
              _tmpCustomerName = _cursor.getString(_cursorIndexOfCustomerName);
            }
            final String _tmpSquareTransactionId;
            if (_cursor.isNull(_cursorIndexOfSquareTransactionId)) {
              _tmpSquareTransactionId = null;
            } else {
              _tmpSquareTransactionId = _cursor.getString(_cursorIndexOfSquareTransactionId);
            }
            final boolean _tmpIsSynced;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsSynced);
            _tmpIsSynced = _tmp_1 != 0;
            _item = new Transaction(_tmpId,_tmpTimestamp,_tmpTotalAmount,_tmpTaxAmount,_tmpItems,_tmpStatus,_tmpPaymentMethod,_tmpFulfillmentStatus,_tmpOrderNumber,_tmpCustomerName,_tmpSquareTransactionId,_tmpIsSynced);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getOfflineTransactions(
      final Continuation<? super List<OfflineTransaction>> $completion) {
    final String _sql = "SELECT * FROM offline_transactions ORDER BY id ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<OfflineTransaction>>() {
      @Override
      @NonNull
      public List<OfflineTransaction> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTransactionJson = CursorUtil.getColumnIndexOrThrow(_cursor, "transactionJson");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<OfflineTransaction> _result = new ArrayList<OfflineTransaction>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final OfflineTransaction _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTransactionJson;
            _tmpTransactionJson = _cursor.getString(_cursorIndexOfTransactionJson);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new OfflineTransaction(_tmpId,_tmpTransactionJson,_tmpTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getTodayMaxOrderNumber(final long startOfDay,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT MAX(orderNumber) FROM transactions WHERE timestamp >= ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startOfDay);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @Nullable
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final Integer _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(0);
            }
            _result = _tmp;
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
