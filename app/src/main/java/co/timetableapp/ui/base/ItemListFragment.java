/*
 * Copyright 2017 Farbod Salamat-Zadeh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.timetableapp.ui.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import co.timetableapp.R;
import co.timetableapp.data.handler.TimetableItemHandler;
import co.timetableapp.model.TimetableItem;

/**
 * A fragment providing default behavior for displaying a list of items.
 *
 * Subclasses must implement abstract methods to define behavior specific to the type of item being
 * displayed, such as how the list is sorted, and what placeholder view is used when there are no
 * items in the list.
 *
 * @param <T> the type of list items to be displayed
 *
 * @see ItemDetailActivity
 * @see ItemEditActivity
 */
public abstract class ItemListFragment<T extends TimetableItem> extends Fragment
        implements ItemListImpl<T> {

    /**
     * A data handler relevant to the type of items being displayed.
     *
     * @see #instantiateDataHandler()
     */
    @Nullable protected TimetableItemHandler<T> mDataHandler;

    /**
     * A list of the data items being displayed of the generic type {@link T}
     */
    @Nullable protected ArrayList<T> mItems;

    /**
     * The RecyclerView adapter used when displaying items to the list.
     */
    @Nullable protected RecyclerView.Adapter mAdapter;

    /**
     * The RecyclerView used to list items in the UI.
     */
    @Nullable protected RecyclerView mRecyclerView;

    private FrameLayout mPlaceholderLayout;

    private View mRootView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_content_list, container, false);

        mDataHandler = instantiateDataHandler();
        setupLayout();

        return mRootView;
    }

    @Override
    public void setupLayout() {
        setupList();

        mPlaceholderLayout = (FrameLayout) mRootView.findViewById(R.id.placeholder);
        refreshPlaceholderStatus();
    }

    @Override
    public void setupList() {
        assert mDataHandler != null;
        mItems = fetchItems();
        sortList();

        mAdapter = setupAdapter();

        mRecyclerView = (RecyclerView) mRootView.findViewById(R.id.recyclerView);
        if (mRecyclerView == null) {
            throw new NullPointerException("could not find RecyclerView (null)");
        }
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mAdapter);
    }

    @NotNull
    @Override
    public ArrayList<T> fetchItems() {
        if (mDataHandler == null) {
            throw new NullPointerException("data handler is null");
        }
        return mDataHandler.getItems(getActivity().getApplication());
    }

    @Override
    public void updateList() {
        assert mItems != null;
        mItems.clear();
        mItems.addAll(fetchItems());
        sortList();

        assert mAdapter != null;
        mAdapter.notifyDataSetChanged();

        refreshPlaceholderStatus();
    }

    @Override
    public void refreshPlaceholderStatus() {
        assert mItems != null;
        assert mRecyclerView != null;

        if (mItems.isEmpty()) {
            mRecyclerView.setVisibility(View.GONE);
            mPlaceholderLayout.setVisibility(View.VISIBLE);

            mPlaceholderLayout.removeAllViews();
            mPlaceholderLayout.addView(getPlaceholderView());

        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            mPlaceholderLayout.setVisibility(View.GONE);
        }
    }

}
