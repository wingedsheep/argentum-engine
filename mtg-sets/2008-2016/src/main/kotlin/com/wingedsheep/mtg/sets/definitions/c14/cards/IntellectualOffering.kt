package com.wingedsheep.mtg.sets.definitions.c14.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Intellectual Offering
 * {4}{U}
 * Instant
 * Choose an opponent. You and that player each draw three cards.
 * Choose an opponent. Untap all nonland permanents you control and all nonland permanents that
 * player controls.
 *
 * Neither opponent is a target (ruling), so each "choose an opponent" is a resolution-time
 * [Effects.ChooseOpponent], read back through [Player.ChosenOpponent]. The second choice
 * overwrites the first, so the two effects may name different opponents. "That player's nonland
 * permanents" runs the same "you control" group untap inside [Effects.ForEachPlayer] over the
 * chosen opponent, which rebinds "you" to that player.
 */
val IntellectualOffering = card("Intellectual Offering") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose an opponent. You and that player each draw three cards.\n" +
        "Choose an opponent. Untap all nonland permanents you control and all nonland permanents that player controls."

    spell {
        val untapYours = Patterns.Group.untapGroup(GroupFilter(GameObjectFilter.NonlandPermanent.youControl()))
        effect = Effects.ChooseOpponent("Choose an opponent: you and that player each draw three cards") then
            Effects.DrawCards(3) then
            Effects.DrawCards(3, EffectTarget.PlayerRef(Player.ChosenOpponent)) then
            Effects.ChooseOpponent("Choose an opponent: untap all nonland permanents you and that player control") then
            untapYours then
            Effects.ForEachPlayer(Player.ChosenOpponent, untapYours)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Mark Winters"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63de9e39-3c16-452f-9f06-4c1298752f57.jpg?1783938871"
        ruling(
            "2014-11-07",
            "You may choose the same opponent for each of the effects, or you may choose different " +
                "opponents. None of the affected players are targets of the spell."
        )
        ruling("2014-11-07", "You choose the opponents for each effect as the spell resolves.")
    }
}
