package com.example.microjob.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Course & Certificate", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF9FAFB))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Course Section
            CourseSection(
                title = "Available Courses",
                courses = listOf(
                    CourseItem("MicroJob Safety Basics", "Learn workplace safety", "2 hrs", Icons.Filled.School),
                    CourseItem("Customer Service 101", "Improve your people skills", "3 hrs", Icons.Filled.School),
                    CourseItem("Digital Payment Guide", "Cashless payment methods", "1 hr", Icons.Filled.School)
                )
            )

            // Certificate Section
            CourseSection(
                title = "My Certificates",
                courses = listOf(
                    CourseItem("Safety Certification", "Completed 15 Aug 2026", "", Icons.Filled.CheckCircle),
                    CourseItem("Service Excellence", "Completed 01 Aug 2026", "", Icons.Filled.CheckCircle)
                )
            )
        }
    }
}

@Composable
private fun CourseSection(title: String, courses: List<CourseItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            courses.forEach { course ->
                CourseRow(course = course)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CourseRow(course: CourseItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF3F4F6))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = course.icon,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = Color(0xFF2563EB)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(course.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(course.description, fontSize = 12.sp, color = Color.Gray)
        }
        if (course.duration.isNotBlank()) {
            Text(
                text = course.duration,
                fontSize = 12.sp,
                color = Color(0xFF2563EB),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private data class CourseItem(
    val name: String,
    val description: String,
    val duration: String,
    val icon: ImageVector
)
