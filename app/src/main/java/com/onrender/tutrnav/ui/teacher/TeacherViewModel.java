package com.onrender.tutrnav.ui.teacher;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.onrender.tutrnav.models.EnrollmentModel;
import com.onrender.tutrnav.models.TuitionModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private final MutableLiveData<List<TuitionModel>> tuitionsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<EnrollmentModel>> enrollmentsLiveData = new MutableLiveData<>(new ArrayList<>());

    // Quick lookup map to match Enrollments to their specific Tuition Class instantly
    private final Map<String, TuitionModel> tuitionMap = new HashMap<>();

    private ListenerRegistration tuitionsListener;
    private ListenerRegistration enrollmentsListener;

    public TeacherViewModel() {
        startListening();
    }

    public void startListening() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        // 1. Listen to Tuitions (Real-time updates for classes created by this teacher)
        tuitionsListener = db.collection("tuitions")
                .whereEqualTo("teacherId", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    List<TuitionModel> list = new ArrayList<>();
                    tuitionMap.clear(); // Clear the old map cache

                    for (DocumentSnapshot doc : value) {
                        TuitionModel t = doc.toObject(TuitionModel.class);
                        if (t != null) {
                            list.add(t);
                            // Cache it in the map so the Dashboard can quickly look up class details (like Title & Fee)
                            tuitionMap.put(t.getTuitionId(), t);
                        }
                    }
                    tuitionsLiveData.setValue(list);
                });

        // 2. Listen to Enrollments (Real-time updates for students joining this teacher's classes)
        enrollmentsListener = db.collection("enrollments")
                .whereEqualTo("teacherId", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    List<EnrollmentModel> list = new ArrayList<>();

                    for (DocumentSnapshot doc : value) {
                        EnrollmentModel e = doc.toObject(EnrollmentModel.class);
                        if (e != null) {

                            // ========================================================
                            // 🔥 THE PERFECTION FIX: 
                            // We MUST override the enrollmentId with the actual 
                            // Firestore Document ID. This ensures that when the teacher
                            // swipes to Accept/Reject, it updates the EXACT document 
                            // on the server without throwing a "NOT_FOUND" network error.
                            // ========================================================
                            e.setEnrollmentId(doc.getId());

                            list.add(e);
                        }
                    }
                    enrollmentsLiveData.setValue(list);
                });
    }

    // --- GETTERS ---
    public LiveData<List<TuitionModel>> getTuitions() {
        return tuitionsLiveData;
    }

    public LiveData<List<EnrollmentModel>> getEnrollments() {
        return enrollmentsLiveData;
    }

    public TuitionModel getTuitionById(String id) {
        return tuitionMap.get(id);
    }

    // --- CLEANUP ---
    @Override
    protected void onCleared() {
        super.onCleared();
        // Prevent memory leaks by removing listeners when the ViewModel is destroyed
        if (tuitionsListener != null) tuitionsListener.remove();
        if (enrollmentsListener != null) enrollmentsListener.remove();
    }
}