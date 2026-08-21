package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule


/**
 * Darkmoss Bridge — Modern Horizons 2 #245
 * (no mana cost) · Artifact Land
 *
 * This land enters tapped.
 * Indestructible
 * {T}: Add {B} or {G}.
 *
 * The reference member of Modern Horizons 2's ten-card "Bridge" cycle — the other nine (Drossforge,
 * Goldmire, Mistvault, Razortide, Rustvale, Silverbluff, Slagwoods, Tanglepool and Thornglint) are
 * this card with two different mana symbols, and their KDoc points back here. Three modelling notes
 * carry the whole cycle:
 *
 *  - **`manaCost = ""`, not `"{0}"`.** A land has no mana cost at all. The DSL reads the blank
 *    string as "has no mana cost"; `"{0}"` would parse into a real, payable zero cost and quietly
 *    make the card castable.
 *  - **"{T}: Add {B} or {G}" is written as two separate mana abilities**, one per colour, rather
 *    than a single ability whose colour is chosen while it resolves — the corpus shape every
 *    dual-tapping land uses (`Golgari Guildgate`). Both satisfy CR 605.1a (no target, could add
 *    mana, not a loyalty ability, and neither cost nor effect moves a card to or from a library), so
 *    each is flagged `manaAbility = true` with [TimingRule.ManaAbility]. Splitting the "or" moves
 *    the player's choice from resolution to activation, which is unobservable in play: a mana
 *    ability never uses the stack and resolves the instant it is activated (CR 605.3b), so nothing
 *    can be done between the choice and the mana arriving either way.
 *  - **Indestructible is the bare [Keyword], not a `KeywordAbility`.** The engine reads
 *    `Keyword.INDESTRUCTIBLE` directly for the CR 702.12b exemptions — destruction and the lethal
 *    damage state-based action (CR 704.5g) both skip the permanent. `Darksteel Citadel` is the
 *    Artifact-Land-plus-indestructible template. Note this does *not* protect the Bridge from
 *    sacrifice or exile, which is the whole reason the cycle is playable.
 *
 * The enters-tapped clause is an [EntersTapped] replacement effect, not a triggered ability: the
 * land is never briefly untapped, so nothing can tap it for mana in response.
 *
 * `colorIdentity` is "BG" — a land's identity comes from the mana symbols in its rules text
 * (CR 903.4), since it has no mana cost to read them from.
 */
val DarkmossBridge = card("Darkmoss Bridge") {
    manaCost = ""
    colorIdentity = "BG"
    typeLine = "Artifact Land"
    oracleText = "This land enters tapped.\n" +
        "Indestructible\n" +
        "{T}: Add {B} or {G}."

    replacementEffect(EntersTapped())
    keywords(Keyword.INDESTRUCTIBLE)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "245"
        artist = "Raoul Vitale"
        flavorText = "The path to power is forged in ruthlessness."
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f11f0be9-b9f7-45d2-9499-c5d849d7289f.jpg?1783926797"
    }
}
