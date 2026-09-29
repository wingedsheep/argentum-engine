package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Voidwing Hybrid
 * {U}{B}
 * Creature — Phyrexian Bat
 * 2/1
 *
 * Flying
 * Toxic 1
 * When you proliferate, return this card from your graveyard to your hand.
 *
 * The return functions only from the graveyard (CR 113.6b), so the trigger is scoped with
 * `triggerZone = Zone.GRAVEYARD`, like Squee, Goblin Nabob.
 */
val VoidwingHybrid = card("Voidwing Hybrid") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Phyrexian Bat"
    power = 2
    toughness = 1
    oracleText = "Flying\n" +
        "Toxic 1 (Players dealt combat damage by this creature also get a poison counter.)\n" +
        "When you proliferate, return this card from your graveyard to your hand."

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.Numeric(Keyword.TOXIC, 1))

    triggeredAbility {
        trigger = Triggers.you.proliferates()
        triggerZone = Zone.GRAVEYARD
        effect = Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.GRAVEYARD)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "221"
        artist = "Abz J Harding"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a48705f-bd8a-43f1-b1fe-14a5c9a83ccd.jpg?1783917995"
    }
}
