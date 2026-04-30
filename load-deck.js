// Simple script to load a deck and create a game with explicit deck list
// Usage: node load-deck.js <deck_name>

const fs = require('fs');

// Load deck configurations
const decks = JSON.parse(fs.readFileSync('example-decks.json', 'utf8'));

function createGameWithDeck(deckName, vsAi = true) {
    const deck = decks[deckName];
    if (!deck) {
        console.error(`Deck "${deckName}" not found. Available decks:`, Object.keys(decks));
        return null;
    }

    console.log(`Creating game with deck: ${deck.name}`);
    console.log('Cards:', deck.cards);

    // This is the payload you'd send to the server
    const gamePayload = {
        type: "createGame",
        deckList: deck.cards,
        vsAi: vsAi
    };

    return gamePayload;
}

// Example usage
if (require.main === module) {
    const deckName = process.argv[2] || 'red_aggro';
    const payload = createGameWithDeck(deckName);
    
    if (payload) {
        console.log('\nGame creation payload:');
        console.log(JSON.stringify(payload, null, 2));
        console.log('\nSend this to the game server to create a game with your explicit deck!');
    }
}

module.exports = { createGameWithDeck, decks };
