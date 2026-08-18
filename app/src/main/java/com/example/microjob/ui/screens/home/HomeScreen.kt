package com.example.microjob.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.microjob.model.Job
import com.example.microjob.model.MalaysianRegions
import com.example.microjob.viewmodel.HomeViewModel
import com.example.microjob.viewmodel.SortOption

/**
 * Home screen: search bar, SDG promotional banner, category chips, a filter
 * bottom sheet and the single-column lazy job list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onJobClick: (Job) -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val filteredJobs by vm.filteredJobs.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by vm.selectedCategory.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val filterState by vm.filterState.collectAsStateWithLifecycle()
    val filterArea by vm.filterArea.collectAsStateWithLifecycle()
    val filterJobType by vm.filterJobType.collectAsStateWithLifecycle()
    val sortOption by vm.sortOption.collectAsStateWithLifecycle()

    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(Unit) {
        vm.loadJobs()
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        // Search area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = vm::onSearchQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = "Search")
                },
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // SDG banner
        HomeBanner(modifier = Modifier.padding(horizontal = 16.dp))

        // Available Jobs + filter icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Available Job",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showFilterSheet = true }) {
                Icon(
                    imageVector = Icons.Filled.FilterList,
                    contentDescription = "Filter",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Single-column lazy list — renders only the visible items regardless
        // of how many jobs are posted.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = filteredJobs,
                key = { it.id }
            ) { job ->
                JobCard(
                    job = job,
                    onClick = { onJobClick(job) }
                )
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            FilterSheetContent(
                categories = categories.map { it.name },
                selectedCategory = selectedCategory,
                filterState = filterState,
                filterArea = filterArea,
                filterJobType = filterJobType,
                sortOption = sortOption,
                onCategoryChange = vm::onCategorySelect,
                onStateChange = { vm.onFilterStateChange(it) },
                onAreaChange = { vm.onFilterAreaChange(it) },
                onJobTypeChange = { vm.onFilterJobTypeChange(it) },
                onSortChange = { vm.onSortOptionChange(it) },
                onClear = {
                    vm.clearFilters()
                    showFilterSheet = false
                },
                onApply = { showFilterSheet = false }
            )
        }
    }
}

@Composable
private fun FilterSheetContent(
    categories: List<String>,
    selectedCategory: String?,
    filterState: String?,
    filterArea: String?,
    filterJobType: String?,
    sortOption: SortOption,
    onCategoryChange: (String?) -> Unit,
    onStateChange: (String?) -> Unit,
    onAreaChange: (String?) -> Unit,
    onJobTypeChange: (String?) -> Unit,
    onSortChange: (SortOption) -> Unit,
    onClear: () -> Unit,
    onApply: () -> Unit,
) {
    val areas = MalaysianRegions.areasOf(filterState ?: "")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Filter Jobs",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Category
        FilterDropdown(
            label = "Category",
            current = selectedCategory,
            options = categories,
            onSelect = { selected ->
                onCategoryChange(selected.ifBlank { null })
            }
        )

        // State
        FilterDropdown(
            label = "State",
            current = filterState,
            options = MalaysianRegions.stateNames,
            onSelect = { selected -> onStateChange(selected.ifBlank { null }) }
        )

        // Area (only when a state is chosen)
        if (filterState != null) {
            FilterDropdown(
                label = "Area",
                current = filterArea,
                options = areas,
                onSelect = { selected -> onAreaChange(selected.ifBlank { null }) }
            )
        }

        // Job type
        FilterDropdown(
            label = "Job type",
            current = filterJobType?.let { if (it == "remote") "Remote" else "On-site" },
            options = listOf("Remote", "On-site"),
            onSelect = { selected ->
                onJobTypeChange(
                    when (selected) {
                        "Remote" -> "remote"
                        "On-site" -> "onsite"
                        else -> null
                    }
                )
            }
        )

        // Price sort
        FilterDropdown(
            label = "Price",
            current = if (sortOption == SortOption.NONE) null else sortOption.label,
            options = SortOption.entries.filter { it != SortOption.NONE }.map { it.label },
            onSelect = { selected ->
                if (selected.isBlank()) {
                    onSortChange(SortOption.NONE)
                } else {
                    onSortChange(SortOption.entries.first { it.label == selected })
                }
            }
        )

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onClear) {
                Text("Clear")
            }
            Button(onClick = onApply) {
                Text("Apply")
            }
        }

        // Extra breathing room below the buttons so they are not too close
        // to the bottom of the sheet (avoids accidental taps).
        Spacer(Modifier.height(48.dp))
    }
}

/** A labeled dropdown: OutlinedButton that opens a DropdownMenu of options. */
@Composable
private fun FilterDropdown(
    label: String,
    current: String?,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = current ?: "All",
                modifier = Modifier.weight(1f),
                color = if (current == null) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
            )
            Text("▾")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All") },
                onClick = {
                    onSelect("")
                    expanded = false
                }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
