package com.onrender.tutrnav.ui.student;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.onrender.tutrnav.models.TuitionModel;

public class StudentSharedViewModel extends ViewModel {
    private final MutableLiveData<TuitionModel> selectedTuition = new MutableLiveData<>();

    public void select(TuitionModel tuition) {
        selectedTuition.setValue(tuition);
    }

    public LiveData<TuitionModel> getSelected() {
        return selectedTuition;
    }
}