#include <iostream>
#include <queue>
#include <string>
using namespace std;

struct Token {
    int token_id;
    string user_name;
    string service_type;
};

class SmartQueue {
    queue<Token> q;
    int nextTokenId = 1;

public:
    void generateToken(string name, string service) {
        Token t = {nextTokenId++, name, service};
        q.push(t);
        cout << "Token generated: " << t.token_id 
             << " for " << t.user_name 
             << " (" << t.service_type << ")\n";
    }

    void viewQueue() {
        if (q.empty()) {
            cout << "Queue is empty.\n";
            return;
        }
        cout << "Current Queue:\n";
        queue<Token> temp = q;
        while (!temp.empty()) {
            Token t = temp.front();
            cout << "Token " << t.token_id << " - " << t.user_name 
                 << " (" << t.service_type << ")\n";
            temp.pop();
        }
    }

    void serveNext() {
        if (q.empty()) {
            cout << "No tokens to serve.\n";
            return;
        }
        Token t = q.front();
        q.pop();
        cout << "Serving Token " << t.token_id 
             << " (" << t.user_name << ")\n";
    }
};

int main() {
    SmartQueue sq;
    int choice;
    string name, service;

    do {
        cout << "\n--- Smart Queue Management ---\n";
        cout << "1. Generate Token\n";
        cout << "2. View Queue\n";
        cout << "3. Serve Next Token\n";
        cout << "4. Exit\n";
        cout << "Enter choice: ";
        cin >> choice;

        switch(choice) {
            case 1:
                cout << "Enter user name: ";
                cin >> name;
                cout << "Enter service type: ";
                cin >> service;
                sq.generateToken(name, service);
                break;
            case 2:
                sq.viewQueue();
                break;
            case 3:
                sq.serveNext();
                break;
            case 4:
                cout << "Exiting...\n";
                break;
            default:
                cout << "Invalid choice.\n";
        }
    } while(choice != 4);

    return 0;
}
