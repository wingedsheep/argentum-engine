package com.wingedsheep.mtg.sets.definitions.voc.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Markov Enforcer
 * {4}{R}{R}
 * Creature — Vampire Soldier
 * 6/6
 * Whenever this creature or another Vampire you control enters, this creature fights up to one
 * target creature an opponent controls.
 * Whenever a creature dealt damage by this creature this turn dies, create a Blood token.
 *
 * Markov Enforcer is itself a Vampire, so "this creature or another Vampire you control" is one
 * Vampire-you-control enters trigger (Evelyn, the Covetous); the "up to one" fight is Thorn
 * Mammoth's. The second ability is Sengir Vampire's dealt-damage-dies trigger: fight damage is
 * recorded like any other damage this creature deals, so a creature it fought and killed (or
 * damaged and that died later this turn) yields a Blood token.
 */
val MarkovEnforcer = card("Markov Enforcer") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire Soldier"
    oracleText = "Whenever this creature or another Vampire you control enters, this creature fights " +
        "up to one target creature an opponent controls.\n" +
        "Whenever a creature dealt damage by this creature this turn dies, create a Blood token. " +
        "(It's an artifact with \"{1}, {T}, Discard a card, Sacrifice this token: Draw a card.\")"
    power = 6
    toughness = 6

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype(Subtype("Vampire")).youControl()).enters()
        val foe = target(TargetFilter.Creature.opponentControls(), optional = true)
        effect = Effects.Fight(EffectTarget.Self, foe)
    }

    triggeredAbility {
        trigger = Triggers.self.damagedCreatureDies()
        effect = Effects.CreateBlood(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "26"
        artist = "Wisnu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/597bc4ae-335d-4d1d-8a57-530291abc540.jpg?1783924998"
    }
}
