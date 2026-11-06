package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.embalm
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Heart-Piercer Manticore — Amonkhet #138
 * {2}{R}{R} · Creature — Manticore · Rare · 4/3
 *
 * When this creature enters, you may sacrifice another creature. When you do, this creature
 * deals damage equal to that creature's power to any target.
 * Embalm {5}{R}
 *
 * Surtland Flinger's shape: the sacrifice is a resolution-time, non-targeting choice, and only an
 * actual sacrifice fires the CR 603.12 "When you do" reflexive ability, whose "any target" is
 * chosen as it goes on the stack. The power is snapshotted while the creature is still on the
 * battlefield (its last known power, per the 2017-04-18 ruling) and read back on resolution.
 */
val HeartPiercerManticore = card("Heart-Piercer Manticore") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Manticore"
    power = 4
    toughness = 3
    oracleText = "When this creature enters, you may sacrifice another creature. When you do, this " +
        "creature deals damage equal to that creature's power to any target.\n" +
        "Embalm {5}{R} ({5}{R}, Exile this card from your graveyard: Create a token that's a copy of it, " +
        "except it's a white Zombie Manticore with no mana cost. Embalm only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ReflexiveTrigger(
            action = Effects.Pipeline {
                val toSacrifice = selectTarget(
                    TargetObject(filter = TargetFilter.CreatureYouControl.other()),
                    nonTargeting = true,
                    prompt = "Choose another creature to sacrifice"
                )
                // Named: the reflexive trigger below reads it once this pipeline has finished.
                storeNumber(DynamicAmounts.powerOf(toSacrifice.asTarget), name = "heartPiercerManticoreDamage")
                run(Effects.SacrificeTarget(toSacrifice.asTarget))
            },
            optional = true,
            descriptionOverride = "You may sacrifice another creature. When you do, Heart-Piercer Manticore " +
                "deals damage equal to that creature's power to any target.",
        ) {
            val any = target(Targets.Any)
            effect = Effects.DealDamage(DynamicAmounts.storedNumber("heartPiercerManticoreDamage"), any)
        }
        description = "When Heart-Piercer Manticore enters, you may sacrifice another creature. When you do, " +
            "Heart-Piercer Manticore deals damage equal to that creature's power to any target."
    }

    embalm("{5}{R}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "138"
        artist = "Scott Murphy"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd64160a-96e0-4593-ba42-c5e8bbfa0b15.jpg?1783936487"
        ruling(
            "2017-04-18",
            "Heart-Piercer Manticore's triggered ability goes on the stack without a target. While that " +
                "ability is resolving, you may sacrifice a creature. If you do, a second ability triggers " +
                "and you pick a target that will be dealt damage. Players may cast spells and activate " +
                "abilities after the creature is sacrificed but before damage is dealt."
        )
        ruling(
            "2017-04-18",
            "The damage-dealing ability triggers only when you sacrifice a creature as a result of the " +
                "instruction of its triggered ability."
        )
        ruling("2017-04-18", "The sacrificed creature's last known existence on the battlefield is checked to determine its power.")
        ruling("2017-04-18", "You can't sacrifice multiple creatures to deal damage multiple times.")
    }
}
