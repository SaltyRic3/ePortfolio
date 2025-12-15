// ProjectTwo.cpp
// ABCU Advising Assistance Program
// Author: Brian Voo | Course: CS 300 | Date: <8/15/2025>
// Description: Loads course data from CSV, prints sorted course list,
// and prints an individual course with its prerequisites.
// Notes: Single-file solution; uses only C++ standard library.

#include <iostream>
#include <fstream>
#include <sstream>
#include <string>
#include <vector>
#include <unordered_map>
#include <algorithm>
#include <cctype>

struct Course {
    std::string number;
    std::string title;
    std::vector<std::string> prereqs;
};

using Catalog = std::unordered_map<std::string, Course>;

// Trim leading/trailing spaces
static inline std::string trim(std::string s) {
    auto notspace = [](int ch){ return !std::isspace(ch); };
    s.erase(s.begin(), std::find_if(s.begin(), s.end(), notspace));
    s.erase(std::find_if(s.rbegin(), s.rend(), notspace).base(), s.end());
    return s;
}

// Convert to uppercase
static inline std::string toUpper(std::string s) {
    std::transform(s.begin(), s.end(), s.begin(),
                   [](unsigned char c){ return std::toupper(c); });
    return s;
}

// Load CSV into catalog
bool loadDataStructure(const std::string& filename, Catalog& catalog) {
    catalog.clear();
    std::ifstream in(filename);
    if (!in.is_open()) {
        std::cout << "Error: could not open " << filename << "\n";
        return false;
    }
    std::string line;
    while (std::getline(in, line)) {
        if (trim(line).empty()) continue;
        std::vector<std::string> cols;
        std::stringstream ss(line);
        std::string cell;
        while (std::getline(ss, cell, ',')) cols.push_back(trim(cell));
        if (cols.size() < 2) continue;

        Course c;
        c.number = toUpper(cols[0]);
        c.title  = cols[1];
        for (size_t i = 2; i < cols.size(); ++i) {
            if (!cols[i].empty()) c.prereqs.push_back(toUpper(cols[i]));
        }
        catalog[c.number] = std::move(c);
    }
    return true;
}

// Print all courses
void printCourseList(const Catalog& catalog) {
    std::vector<std::string> keys;
    for (auto& kv : catalog) keys.push_back(kv.first);
    std::sort(keys.begin(), keys.end());

    std::cout << "Here is a sample schedule:\n\n";
    for (auto& k : keys) {
        const Course& c = catalog.at(k);
        std::cout << c.number << ", " << c.title << "\n";
    }
    std::cout << "\n";
}

// Print single course
void printCourseInfo(const Catalog& catalog, std::string courseNum) {
    courseNum = toUpper(trim(courseNum));
    auto it = catalog.find(courseNum);
    if (it == catalog.end()) {
        std::cout << "Course not found.\n\n";
        return;
    }
    const Course& c = it->second;
    std::cout << c.number << ", " << c.title << "\n";
    if (c.prereqs.empty()) {
        std::cout << "Prerequisites: None\n\n";
    } else {
        std::cout << "Prerequisites: ";
        for (size_t i = 0; i < c.prereqs.size(); ++i) {
            if (i) std::cout << ", ";
            std::cout << c.prereqs[i];
        }
        std::cout << "\n\n";
    }
}

void printMenu() {
    std::cout << "    1. Load Data Structure.\n";
    std::cout << "    2. Print Course List.\n";
    std::cout << "    3. Print Course.\n";
    std::cout << "    9. Exit\n\n";
}

int main() {
    Catalog catalog;
    bool loaded = false;

    std::cout << "Welcome to the course planner.\n\n";
    printMenu();

    for (;;) {
        std::cout << "What would you like to do? ";
        std::string choice;
        if (!std::getline(std::cin, choice)) break;
        choice = trim(choice);

        if (choice == "1") {
            // Always load the given file automatically
            if (loadDataStructure("CS 300 ABCU_Advising_Program_Input.csv", catalog)) {
                loaded = true;
            }
            std::cout << "\n";
            printMenu();
        }
        else if (choice == "2") {
            if (!loaded) {
                loadDataStructure("CS 300 ABCU_Advising_Program_Input.csv", catalog);
                loaded = true;
            }
            printCourseList(catalog);
            printMenu();
        }
        else if (choice == "3") {
            if (!loaded) {
                loadDataStructure("CS 300 ABCU_Advising_Program_Input.csv", catalog);
                loaded = true;
            }
            std::cout << "What course do you want to know about? ";
            std::string cn;
            std::getline(std::cin, cn);
            printCourseInfo(catalog, cn);
            printMenu();
        }
        else if (choice == "9") {
            std::cout << "Thank you for using the course planner!\n";
            return 0;
        }
        else {
            std::cout << choice << " is not a valid option.\n\n";
            printMenu();
        }
    }
}
