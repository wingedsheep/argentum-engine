package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fateful Handoff
 * {3}{B}
 * Sorcery
 * Draw cards equal to the mana value of target artifact or creature you control. An opponent gains
 * control of that permanent.
 *
 * Modelling notes:
 * - The draw reads the target's mana value while it is still on the battlefield, before control
 *   changes; a permanent with {X} in its cost counts X as 0 and a prototyped creature uses its
 *   prototype cost (rulings) — both fall out of the engine's mana-value read.
 * - "An opponent" is not targeted: the caster picks while the spell resolves, so
 *   [Effects.ChooseOpponent] records the pick and [Effects.GiveControl] reads it back through
 *   [Player.ChosenOpponent] (the Wishclaw Talisman shape). In two-player games the pick is forced.
 */
val FatefulHandoff = card("Fateful Handoff") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Draw cards equal to the mana value of target artifact or creature you control. " +
        "An opponent gains control of that permanent."

    spell {
        val permanent = target(TargetFilter.CreatureOrArtifact.youControl())
        effect = Effects.DrawCards(DynamicAmounts.manaValueOf(permanent)) then
            Effects.ChooseOpponent("Choose an opponent to gain control of that permanent") then
            Effects.GiveControl(
                permanent = permanent,
                newController = EffectTarget.PlayerRef(Player.ChosenOpponent)
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "94"
        artist = "Gaboleps"
        flavorText = "\"Get the sylex to Urza at any cost. Tell him to fill it with memories of the land.\"\n" +
            "—Ashnod, to Tawnos"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0bbed8f9-3615-428b-8d0d-18291c5a99be.jpg?1783920092"
        ruling(
            "2022-10-14",
            "The mana value of an artifact creature that was cast as a prototype is based on the " +
                "prototype mana cost rather than the card's usual mana cost."
        )
        ruling("2022-10-14", "If an artifact or creature on the battlefield has {X} in its mana cost, X is 0.")
    }
}
