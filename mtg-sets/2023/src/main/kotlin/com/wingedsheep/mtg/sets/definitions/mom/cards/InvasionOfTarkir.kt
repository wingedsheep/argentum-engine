package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Invasion of Tarkir // Defiant Thundermaw — March of the Machine #149.
 * {1}{R} · Battle — Siege · defense 5 // Creature — Dragon 4/4
 *
 * Front: a [Effects.ReflexiveTrigger] (CR 603.12). The action reveals any number (zero allowed) of
 * Dragon cards from hand and stores them; "when you do" then goes on the stack with its own
 * "any other target" (chosen then, per the ruling) and deals the revealed count plus 2.
 * Back: the attacking Dragon is the damage source.
 */
private val revealedDragons = CollectionSlot("revealedDragons")

private val InvasionOfTarkirFront = card("Invasion of Tarkir") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Battle — Siege"
    startingDefense = 5
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, reveal any number of Dragon cards from your hand. When you do, " +
        "this Siege deals X plus 2 damage to any other target, where X is the number of cards " +
        "revealed this way. (X can be 0.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ReflexiveTrigger(
            action = Effects.Pipeline {
                val dragons = gather(
                    CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Any.withSubtype(Subtype.DRAGON))
                )
                val chosen = chooseAnyNumber(
                    dragons,
                    prompt = "Reveal any number of Dragon cards from your hand",
                    name = "revealedDragons",
                )
                reveal(chosen)
            },
            optional = false,
            descriptionOverride = "Reveal any number of Dragon cards from your hand. When you do, this " +
                "Siege deals X plus 2 damage to any other target, where X is the number of cards revealed this way.",
        ) {
            val victim = target(TargetOther(Targets.Any))
            effect = Effects.DealDamage(revealedDragons.count + 2, victim)
        }
        description = "When this Siege enters, reveal any number of Dragon cards from your hand. When you " +
            "do, this Siege deals X plus 2 damage to any other target, where X is the number of cards " +
            "revealed this way. (X can be 0.)"
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "149"
        artist = "Darren Tan"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a98f71a0-7c99-4cee-8f70-877e698cca84.jpg?1783916995"
        ruling("2023-04-14", "Invasion of Tarkir's enters-the-battlefield ability triggers and goes on the stack without a target. As it resolves, you'll reveal any number of Dragon cards from your hand. As the card reminds you, that number can be zero. Then the second \"reflexive\" triggered ability will trigger. You'll choose the target for that second ability at that time.")
        ruling("2023-04-14", "If you don't reveal any Dragon cards from your hand, the reflexive triggered ability will cause Invasion of Tarkir to deal 2 damage to the target of the ability.")
    }
}

private val DefiantThundermaw = card("Defiant Thundermaw") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying, trample\nWhenever a Dragon you control attacks, it deals 2 damage to any target."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withSubtype(Subtype.DRAGON).youControl()).attacks()
        val t = target(Targets.Any)
        effect = Effects.DealDamage(2, t, damageSource = EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "149"
        artist = "Darren Tan"
        flavorText = "\"The Phyrexians are fools if they believe they can weather such a storm.\"\n—Sarkhan Vol"
        imageUri = "https://cards.scryfall.io/normal/back/a/9/a98f71a0-7c99-4cee-8f70-877e698cca84.jpg?1783916995"
    }
}

val InvasionOfTarkir: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfTarkirFront,
    backFace = DefiantThundermaw,
)
