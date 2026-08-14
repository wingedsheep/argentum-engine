package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Axebane Ferox — Murders at Karlov Manor #153
 * {2}{G}{G} · Creature — Beast · 4/4 · Rare
 *
 * Deathtouch, haste
 * Ward—Collect evidence 4.
 *
 * A hasty 4/4 deathtoucher that also taxes removal — but in cards rather than mana, and out of the
 * *opponent's* graveyard. Early on that is nearly a hard protection: an empty graveyard cannot reach
 * 4 at all, and CR 701.59b says a player who can't reach the total **can't choose to collect
 * evidence**, so the removal spell is countered with no prompt. Late, with a stocked graveyard, it
 * costs a real chunk of the opponent's recursion instead.
 *
 * The ward cost is [KeywordAbility.wardCollectEvidence], which this card introduces — the fourth of
 * the four collect-evidence contexts and the only one that is a *ward* cost. It exiles any number
 * of cards from the paying player's graveyard with total mana value 4 or greater; the constraint is
 * a **sum**, not a count, so a graveyard of five lands is never enough and over-paying (exiling a
 * 6-drop) is legal. It is an **unlinked** cost: nothing on this card asks whether evidence was
 * collected, so it stamps no `ChoiceSlot` and satisfies no `WasEvidenceCollected` condition.
 */
val AxebaneFerox = card("Axebane Ferox") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 4
    oracleText = "Deathtouch, haste\n" +
        "Ward—Collect evidence 4. (Whenever this creature becomes the target of a spell or " +
        "ability an opponent controls, counter it unless that player exiles cards with total mana " +
        "value 4 or greater from their graveyard.)"

    keywords(Keyword.DEATHTOUCH, Keyword.HASTE)
    keywordAbility(KeywordAbility.wardCollectEvidence(4))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "153"
        artist = "Maxime Minard"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/610a0de4-a4f7-446b-8477-00c917cb4789.jpg?1783912873"

        ruling(
            "2024-02-02",
            "If you can't exile enough cards to meet or exceed the required mana value, you can't " +
                "choose to collect evidence at all."
        )
        ruling(
            "2024-02-02",
            "Once you've announced that you're casting a spell, players can't take actions until " +
                "you've finished doing so. Notably, opponents can't try to remove cards from your " +
                "graveyard to stop you from collecting evidence."
        )
    }
}
