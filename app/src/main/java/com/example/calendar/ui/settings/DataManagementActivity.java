package com.example.calendar.ui.settings;

import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.calendar.R;
import com.example.calendar.databinding.ActivityDataManagementBinding;
import com.example.calendar.domain.model.ScheduleBackupOverview;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DataManagementActivity extends AppCompatActivity {
    private static final DateTimeFormatter FILE_NAME_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.CHINA);

    private ActivityDataManagementBinding binding;
    private DataManagementViewModel viewModel;

    private final ActivityResultLauncher<String> createDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), this::handleExportUri);

    private final ActivityResultLauncher<String[]> openDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleImportUri);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDataManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new androidx.lifecycle.ViewModelProvider(this, new DataManagementViewModelFactory(this))
                .get(DataManagementViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.exportButton.setOnClickListener(v -> createDocumentLauncher.launch(buildBackupFileName()));
        binding.importButton.setOnClickListener(v -> openDocumentLauncher.launch(new String[]{"application/json", "*/*"}));

        viewModel.getOverviewLiveData().observe(this, this::bindOverview);
    }

    private void bindOverview(ScheduleBackupOverview overview) {
        if (overview == null) {
            return;
        }
        binding.summaryValue.setText(getString(
                R.string.data_management_summary_format,
                overview.getScheduleCount(),
                overview.getRecurrenceSeriesCount(),
                overview.getRecurrenceExceptionCount()
        ));
    }

    private void handleExportUri(Uri uri) {
        if (uri == null) {
            return;
        }
        try {
            writeUtf8Text(uri, viewModel.buildExportJson());
            Snackbar.make(binding.getRoot(), R.string.data_management_export_success, Snackbar.LENGTH_SHORT).show();
        } catch (IOException exception) {
            Snackbar.make(binding.getRoot(), R.string.data_management_io_error, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void handleImportUri(Uri uri) {
        if (uri == null) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.data_management_import_confirm_title)
                .setMessage(R.string.data_management_import_confirm_message)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> performImport(uri))
                .show();
    }

    private void performImport(@NonNull Uri uri) {
        try {
            String json = readUtf8Text(uri);
            viewModel.importBackupJson(json);
            Snackbar.make(binding.getRoot(), R.string.data_management_import_success, Snackbar.LENGTH_SHORT).show();
        } catch (IllegalArgumentException exception) {
            Snackbar.make(binding.getRoot(), R.string.data_management_invalid_backup, Snackbar.LENGTH_SHORT).show();
        } catch (IOException exception) {
            Snackbar.make(binding.getRoot(), R.string.data_management_io_error, Snackbar.LENGTH_SHORT).show();
        }
    }

    private String buildBackupFileName() {
        return getString(R.string.data_management_export_filename_prefix)
                + "-"
                + FILE_NAME_TIME_FORMATTER.format(LocalDateTime.now())
                + ".json";
    }

    private void writeUtf8Text(@NonNull Uri uri, @NonNull String content) throws IOException {
        try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
            if (outputStream == null) {
                throw new IOException("Cannot open output stream.");
            }
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }
    }

    @NonNull
    private String readUtf8Text(@NonNull Uri uri) throws IOException {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                throw new IOException("Cannot open input stream.");
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }
                return builder.toString();
            }
        }
    }
}
