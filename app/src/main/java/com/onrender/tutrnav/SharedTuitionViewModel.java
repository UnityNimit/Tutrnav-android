package com.onrender.tutrnav;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedTuitionViewModel extends ViewModel {
    private final MutableLiveData<TuitionModel> selectedTuition = new MutableLiveData<>();

    public void select(TuitionModel tuition) {
        selectedTuition.setValue(tuition);
    }

    public LiveData<TuitionModel> getSelected() {
        return selectedTuition;
    }
}