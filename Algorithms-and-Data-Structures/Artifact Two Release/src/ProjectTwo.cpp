#include <iostream>
#include <fstream>
#include <sstream>
#include <string>
#include <vector>
#include <unordered_map>
#include <algorithm>
#include <cctype>

// -------------------------
// Core course data structure
// -------------------------
struct Course {
    std::string number;
    std::string title;
    std::vector<std::string> prereqs;
};

using Catalog = std::unordered_map<std::string, Course>;

// -------------------------
// Binary Search Tree for Courses
// -------------------------
// Each node stores a Course and left/right pointers.
// The tree is ordered by course.number in ascending order.
struct BSTNode {
    Course course;
    BSTNode* left;
    BSTNode* right;

    explicit BSTNode(const Course& c)
        : course(c), left(nullptr), right(nullptr) {}
};

// Insert a course into the BST.
// Time complexity: O(h), where h is tree height (average O(log n) if balanced).
BSTNode* insertBST(BSTNode* root, const Course& c) {
    if (!root) {
        return new BSTNode(c);
    }

    if (c.number < root->course.number) {
        root->left = insertBST(root->left, c);
    }
    else if (c.number > root->course.number) {
        root->right = insertBST(root->right, c);
    }
    else {
        // Duplicate key: update the stored course data
        root->course = c;
    }
    return root;
}

// Search for a course by course number in the BST.
// Time complexity: O(h), average O(log n) if the tree is reasonably balanced.
Course* searchBST(BSTNode* root, const std::string& courseNum) {
    if (!root) {
        return nullptr;
    }
    if (courseNum == root->course.number) {
        return &root->course;
    }
    if (courseNum < root->course.number) {
        return searchBST(root->left, courseNum);
    }
    else {
        return searchBST(root->right, courseNum);
    }
}

// In-order traversal to print courses in sorted order.
// Time complexity: O(n), visiting each node exactly once.
void inorderPrintCourses(BSTNode* root) {
    if (!root) {
        return;
    }
    inorderPrintCourses(root->left);
    std::cout << root->course.number << ", " << root->course.title << "\n";
    inorderPrintCourses(root->right);
}

// Recursively free all nodes in the BST.
void deleteBST(BSTNode* root) {
    if (!root) return;
    deleteBST(root->left);
    deleteBST(root->right);
    delete root;
}

// Build a BST from the catalog (unordered_map).
// This lets us demonstrate both hash-based lookup and tree-based algorithms.
BSTNode* buildBSTFromCatalog(const Catalog& catalog) {
    BSTNode* root = nullptr;
    for (const auto& kv : catalog) {
        root = insertBST(root, kv.second);
    }
    return root;
}

// -------------------------
// Utility string functions
// -------------------------

// Trim leading/trailing spaces
static inline std::string trim(std::string s) {
    auto notspace = [](int ch) { return !std::isspace(ch); };
    s.erase(s.begin(), std::find_if(s.begin(), s.end(), notspace));
    s.erase(std::find_if(s.rbegin(), s.rend(), notspace).base(), s.end());
    return s;
}

// Convert to uppercase
static inline std::string toUpper(std::string s) {
    std::transform(s.begin(), s.end(), s.begin(),
                   [](unsigned char c) { return std::toupper(c); });
    return s;
}

// -------------------------
// Load CSV into catalog (hash table)
// -------------------------
// Time complexity: O(n) to read n lines and insert into the unordered_map
// (average-case O(1) insertion per element).
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

// -------------------------
// Printing / query helpers using BST
// -------------------------

// Print all courses in sorted order using the BST.
void printCourseList(BSTNode* bstRoot) {
    if (!bstRoot) {
        std::cout << "No courses loaded.\n\n";
        return;
    }

    std::cout << "Here is a sample schedule:\n\n";
    inorderPrintCourses(bstRoot);
    std::cout << "\n";
}

// Print single course information using a BST search for the query.
void printCourseInfo(BSTNode* bstRoot, std::string courseNum) {
    if (!bstRoot) {
        std::cout << "No courses loaded.\n\n";
        return;
    }

    courseNum = toUpper(trim(courseNum));
    Course* c = searchBST(bstRoot, courseNum);
    if (!c) {
        std::cout << "Course not found.\n\n";
        return;
    }

    std::cout << c->number << ", " << c->title << "\n";
    if (c->prereqs.empty()) {
        std::cout << "Prerequisites: None\n\n";
    }
    else {
        std::cout << "Prerequisites: ";
        for (size_t i = 0; i < c->prereqs.size(); ++i) {
            if (i) std::cout << ", ";
            std::cout << c->prereqs[i];
        }
        std::cout << "\n\n";
    }
}

// -------------------------
// Menu + main program
// -------------------------

void printMenu() {
    std::cout << "    1. Load Data Structure.\n";
    std::cout << "    2. Print Course List.\n";
    std::cout << "    3. Print Course.\n";
    std::cout << "    9. Exit\n\n";
}

int main() {
    Catalog catalog;          // hash table for O(1) average lookups
    BSTNode* bstRoot = nullptr;  // BST for ordered traversal and logarithmic search
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
                // Rebuild BST from the updated catalog
                if (bstRoot) {
                    deleteBST(bstRoot);
                    bstRoot = nullptr;
                }
                bstRoot = buildBSTFromCatalog(catalog);
                loaded = true;
                std::cout << "Data loaded into hash table and binary search tree.\n\n";
            }
            printMenu();
        }
        else if (choice == "2") {
            if (!loaded) {
                if (loadDataStructure("CS 300 ABCU_Advising_Program_Input.csv", catalog)) {
                    if (bstRoot) {
                        deleteBST(bstRoot);
                        bstRoot = nullptr;
                    }
                    bstRoot = buildBSTFromCatalog(catalog);
                    loaded = true;
                }
            }
            printCourseList(bstRoot);
            printMenu();
        }
        else if (choice == "3") {
            if (!loaded) {
                if (loadDataStructure("CS 300 ABCU_Advising_Program_Input.csv", catalog)) {
                    if (bstRoot) {
                        deleteBST(bstRoot);
                        bstRoot = nullptr;
                    }
                    bstRoot = buildBSTFromCatalog(catalog);
                    loaded = true;
                }
            }
            std::cout << "What course do you want to know about? ";
            std::string cn;
            std::getline(std::cin, cn);
            printCourseInfo(bstRoot, cn);
            printMenu();
        }
        else if (choice == "9") {
            std::cout << "Thank you for using the course planner!\n";
            break;
        }
        else {
            std::cout << choice << " is not a valid option.\n\n";
            printMenu();
        }
    }

    // Clean up dynamically allocated BST before exiting.
    deleteBST(bstRoot);
    return 0;
}
