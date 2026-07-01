package com.example.apartmentmanagement.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.adapters.EventAdapter;
import com.example.apartmentmanagement.models.Event;
import com.example.apartmentmanagement.utils.UserHelper;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class EventsFragment extends Fragment {

    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private List<Event> allEvents      = new ArrayList<>();
    private List<Event> filteredEvents = new ArrayList<>();
    private Set<String> registeredIds  = new HashSet<>();

    private TextView tvSubtitle, tvNoEvents;
    private ChipGroup chipGroup;
    private View rootView;

    private FirebaseFirestore db;
    private String residentId = "";
    private String currentFilter = "all";

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db       = FirebaseFirestore.getInstance();
        rootView = view;

        tvSubtitle = view.findViewById(R.id.tvEventSubtitle);
        tvNoEvents = view.findViewById(R.id.tvNoEvents);
        chipGroup  = view.findViewById(R.id.chipGroupEvent);
        recyclerView = view.findViewById(R.id.recyclerEvents);

        adapter = new EventAdapter(requireContext(), filteredEvents,
                registeredIds, event -> showRegisterDialog(event));
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        chipGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            if      (id == R.id.chipEventAll)      currentFilter = "all";
            else if (id == R.id.chipEventGift)     currentFilter = "gift";
            else if (id == R.id.chipEventActivity) currentFilter = "activity";
            else if (id == R.id.chipEventService)  currentFilter = "service";
            applyFilter();
        });

        UserHelper.getIds((userId, resId) -> {
            residentId = resId;
            loadRegistrations();
            loadEvents();
        }, msg -> Snackbar.make(rootView, msg, Snackbar.LENGTH_SHORT).show());
    }

    private void loadRegistrations() {
        db.collection("event_registrations")
                .whereEqualTo("resident_id", residentId)
                .get()
                .addOnSuccessListener(snap -> {
                    registeredIds.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        String eventId = doc.getString("event_id");
                        if (eventId != null) registeredIds.add(eventId);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void loadEvents() {
        db.collection("events")
                .get()
                .addOnSuccessListener(snap -> {
                    allEvents.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Event e = doc.toObject(Event.class);
                        if (e.getId() == null) e.setId(doc.getId());
                        allEvents.add(e);
                    }
                    applyFilter();
                    long upcoming = allEvents.stream()
                            .filter(e -> "upcoming".equals(e.getStatus())).count();
                    tvSubtitle.setText(upcoming + " sự kiện sắp diễn ra");
                });
    }

    private void applyFilter() {
        filteredEvents.clear();
        for (Event e : allEvents) {
            if ("all".equals(currentFilter) || currentFilter.equals(e.getType())) {
                filteredEvents.add(e);
            }
        }
        adapter.notifyDataSetChanged();
        tvNoEvents.setVisibility(filteredEvents.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showRegisterDialog(Event event) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Đăng ký: " + event.getTitle())
                .setMessage(
                        "📅 " + event.getEvent_date() + " lúc " + event.getEvent_time() + "\n" +
                                "📍 " + event.getLocation() + "\n\n" +
                                event.getDescription()
                )
                .setPositiveButton("Xác nhận đăng ký", (d, w) -> registerEvent(event))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void registerEvent(Event event) {
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()).format(Calendar.getInstance().getTime());

        Map<String, Object> data = new HashMap<>();
        data.put("event_id",    event.getId());
        data.put("resident_id", residentId);
        data.put("status",      "confirmed");
        data.put("note",        "");
        data.put("created_at",  now);

        db.collection("event_registrations").add(data)
                .addOnSuccessListener(ref -> {
                    registeredIds.add(event.getId());
                    adapter.notifyDataSetChanged();
                    Snackbar.make(rootView,
                            "✓ Đã đăng ký " + event.getTitle(),
                            Snackbar.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Snackbar.make(rootView, "Lỗi đăng ký", Snackbar.LENGTH_SHORT).show());
    }
}