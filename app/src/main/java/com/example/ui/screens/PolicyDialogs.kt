package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrivacyPolicyDialog(isEnglish: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isEnglish) "Privacy Policy" else "প্রাইভেসি পলিসি",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.85f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isEnglish) {
                    Text(
                        text = "Last updated: July 2026",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PolicySection(
                        title = "1. Information We Collect",
                        body = "Distro-Book is designed as a hybrid ledger system. We collect product information, sales history, shop details, outstanding customer dues (baki), and user profile details (such as name, phone, email, and business details). This information is primarily stored locally on your device."
                    )
                    PolicySection(
                        title = "2. Data Storage & Local Security",
                        body = "All your business transaction logs, sales records, and ledger records are securely stored in your device's internal Room SQLite database. We do not automatically send your business transaction details to our private servers. You are responsible for keeping your physical device secured to prevent unauthorized access."
                    )
                    PolicySection(
                        title = "3. Google Drive Backup & Cloud Sync",
                        body = "If you explicitly enable Google Drive Backup, the application will securely upload encrypted copies of your database payload to your private Google Drive account. We do not access, share, or sell this backup data. It remains fully under your own Google Account control."
                    )
                    PolicySection(
                        title = "4. Permissions Requested",
                        body = "• Camera: Required to capture shop avatars or customize item graphics.\n• Internet & Network: Required for standard cloud synchronization, error monitoring, and serving non-intrusive Google AdMob banners (free sponsored ads) which help keep the service active."
                    )
                    PolicySection(
                        title = "5. Contact Information",
                        body = "If you have any questions, suggestions, or concerns regarding your privacy or data usage, feel free to contact our developer team through WhatsApp or Email in the developer section."
                    )
                } else {
                    Text(
                        text = "সর্বশেষ আপডেট: জুলাই ২০২৬",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PolicySection(
                        title = "১. তথ্য সংগ্রহ ও ব্যবহার",
                        body = "ডিস্ট্রো-বুক অ্যাপটি একটি লেজার এবং প্রোডাক্ট ডিস্ট্রিবিউশন বুক হিসেবে কাজ করে। আমরা আপনার প্রোডাক্টের নাম, মূল্য, স্টক, দোকানের প্রোফাইল, ফোন নম্বর, ইমেইল এবং কাস্টমারদের বকেয়া (বাকি) হিসাবের তথ্য সংগ্রহ করে থাকি। এই সমস্ত তথ্য মূলত আপনার নিজের ডিভাইসেই জমা থাকে।"
                    )
                    PolicySection(
                        title = "২. ডেটা স্টোরেজ ও লোকাল নিরাপত্তা",
                        body = "আপনার ব্যবসার সমস্ত কেনা-বেচার বিবরণ ও হিসাব আপনার ডিভাইসের SQLite ডাটাবেসে অত্যন্ত সুরক্ষিতভাবে সংরক্ষিত থাকে। আমরা কোনোভাবেই আপনার সম্মতি ছাড়া এই তথ্য আমাদের নিজস্ব সার্ভারে পাঠাই না। আপনার ডিভাইসটির নিরাপত্তা বজায় রাখার দায়িত্ব সম্পূর্ণ আপনার নিজের।"
                    )
                    PolicySection(
                        title = "৩. গুগল ড্রাইভ ব্যাকআপ ও ক্লাউড সিঙ্ক",
                        body = "আপনি যদি নিজে থেকে গুগল ড্রাইভ ব্যাকআপ অপশনটি চালু করেন, তবেই কেবল আপনার ব্যাকআপ ফাইলটি আপনার গুগল ড্রাইভ অ্যাকাউন্টে নিরাপদে সংরক্ষণ করা হবে। আমরা কোনোভাবেই এই ব্যাকআপ ফাইল এক্সেস করি না বা কোনো থার্ড-পার্টির সাথে শেয়ার করি না। এই ডাটার সম্পূর্ণ নিয়ন্ত্রণ আপনার নিজের কাছেই থাকে।"
                    )
                    PolicySection(
                        title = "৪. প্রয়োজনীয় পারমিশন সমূহ",
                        body = "• ক্যামেরা পারমিশন: প্রোফাইল পিকচার বা দোকানের ছবি যুক্ত করার জন্য প্রয়োজন হতে পারে।\n• ইন্টারনেট পারমিশন: গুগল ড্রাইভে ব্যাকআপ রাখার জন্য এবং অ্যাপে স্পনসরড বিজ্ঞাপন প্রদর্শনের জন্য প্রয়োজন হয়।"
                    )
                    PolicySection(
                        title = "৫. আমাদের সাথে যোগাযোগ",
                        body = "আপনার প্রাইভেসি পলিসি বা ডেটা সংক্রান্ত কোনো প্রশ্ন বা পরামর্শ থাকলে ডেভেলপার সেকশনে থাকা হোয়াটসঅ্যাপ লিংক বা ইমেইলের মাধ্যমে সরাসরি আমাদের সাথে যোগাযোগ করতে পারেন।"
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isEnglish) "OK" else "ঠিক আছে", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun TermsAndConditionsDialog(isEnglish: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Gavel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isEnglish) "Terms & Conditions" else "ব্যবহারের শর্তাবলী",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.85f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isEnglish) {
                    Text(
                        text = "Last updated: July 2026",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PolicySection(
                        title = "1. Agreement to Terms",
                        body = "By accessing or using Distro-Book, you agree to be bound by these Terms and Conditions. If you do not agree to all of these terms, do not use the application."
                    )
                    PolicySection(
                        title = "2. Purpose of the Application",
                        body = "Distro-Book is a business utility tool provided to merchants, shop owners, and distributors to keep records of sales, stock, and dues. It is intended for bookkeeping purposes and should not be used as a substitute for professional legal or accounting advice."
                    )
                    PolicySection(
                        title = "3. User Responsibility & Liability",
                        body = "• You are solely responsible for verifying the accuracy of all financial calculations, outstanding dues, and order amounts before completing transactions with shopkeepers or distributors.\n• The application developer is not liable for any financial disputes, customer disagreements, or operational mistakes arising from the use of this software."
                    )
                    PolicySection(
                        title = "4. Data Backups & Safeguards",
                        body = "Since all data is primarily stored on your local device, we are not responsible for any data corruption, hardware failure, or theft resulting in information loss. We highly recommend using our 'Unified Backup & Restore' feature daily to secure your database."
                    )
                    PolicySection(
                        title = "5. Modification of Service",
                        body = "We reserve the right to modify, suspend, or discontinue any feature, service, or part of the application at any time without notice."
                    )
                } else {
                    Text(
                        text = "সর্বশেষ আপডেট: জুলাই ২০২৬",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PolicySection(
                        title = "১. ব্যবহারের শর্তাবলী অনুমোদন",
                        body = "ডিস্ট্রো-বুক অ্যাপটি ব্যবহার করার মাধ্যমে আপনি আমাদের ব্যবহারের সমস্ত শর্ত মেনে নিচ্ছেন। আপনি যদি এই শর্তাবলীতে সম্মত না হন, তবে দয়া করে এই অ্যাপ্লিকেশনটি ব্যবহার করা থেকে বিরত থাকুন।"
                    )
                    PolicySection(
                        title = "২. অ্যাপ্লিকেশনটির উদ্দেশ্য",
                        body = "ডিস্ট্রো-বুক মূলত খুচরা বিক্রেতা, পাইকারি বিক্রেতা এবং ডিস্ট্রিবিউটরদের ব্যবসা সহজ করার একটি হিসাবরক্ষণ ও ডিস্ট্রিবিউশন সহকারী টুল। এটি কেবল হিসাব সংরক্ষণের কাজে ব্যবহৃত হবে এবং একে কোনো প্রাতিষ্ঠানিক পেশাদার ফাইনান্সিয়াল এডভাইস হিসেবে ধরা যাবে না।"
                    )
                    PolicySection(
                        title = "৩. ব্যবহারকারীর দায়িত্ব ও দায়বদ্ধতা",
                        body = "• ডিস্ট্রিবিউটর বা দোকানদারদের সাথে লেনদেনের হিসাব চূড়ান্ত করার পূর্বে সমস্ত হিসাব, বকেয়া, এবং মোট টাকার পরিমাণটি নিজে যাচাই করে নেওয়া সম্পূর্ণ আপনার নিজের দায়িত্ব।\n• এই অ্যাপ্লিকেশনটির হিসাব বা ফলাফলের কারণে হওয়া কোনো প্রকারের ব্যবসায়িক বিরোধ বা লেনদেনের আর্থিক লোকসানের জন্য অ্যাপ ডেভেলপার কোনোভাবেই দায়ী থাকবে না।"
                    )
                    PolicySection(
                        title = "৪. ব্যাকআপ ও ডেটা হারানোর ঝুঁকি",
                        body = "আপনার সমস্ত ডেটা মূলত আপনার লোকাল ডিভাইসেই জমা থাকে। তাই ফোন হারিয়ে যাওয়া, নষ্ট হওয়া বা রিফ্যাক্টরিং-এর কারণে ডেটা হারিয়ে গেলে ডেভেলপার কোনো দায় নেবে না। ডেটা সুরক্ষিত রাখতে প্রতিদিন নিয়ম করে লোকাল ব্যাকআপ অথবা গুগল ড্রাইভ ব্যাকআপ নিয়ে রাখার জোর পরামর্শ দেওয়া যাচ্ছে।"
                    )
                    PolicySection(
                        title = "৫. পরিবর্তন ও সংশোধন",
                        body = "আমরা যেকোনো সময় অ্যাপের ফিচার পরিবর্তন, বন্ধ বা নতুন নিয়মকানুন যুক্ত করার অধিকার সংরক্ষণ করি।"
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isEnglish) "OK" else "ঠিক আছে", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun PolicySection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
    }
}
