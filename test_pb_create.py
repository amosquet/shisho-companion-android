import requests

url = "https://pocketbase.amcloud.dev/api/collections/shisho_users/records"
payload = {
    "email": "test_user1234@example.com",
    "password": "Password123!",
    "passwordConfirm": "Password123!",
    "discord_id": "",
    "preferences": {}
}

response = requests.post(url, json=payload)
print(response.status_code)
print(response.json())
