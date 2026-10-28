package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Resolute Strike — Zendikar Rising #35
 * {W} · Instant · Common
 *
 * Target creature gets +2/+2 until end of turn. If it's a Warrior, you may attach an Equipment you
 * control to it.
 *
 * Same shape as Vow to Erebor: the Warrior check is made on resolution against the one target, and
 * the Equipment is chosen (not targeted) with `chooseUpTo(1)`, whose "up to" is the "you may".
 * `includeAttachments = true` lets an Equipment already attached to another creature be moved.
 * The target may be any creature — an opponent's Warrior can receive your Equipment too.
 */
val ResoluteStrike = card("Resolute Strike") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 until end of turn. " +
        "If it's a Warrior, you may attach an Equipment you control to it."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 2, creature, Duration.EndOfTurn) then
            Effects.If(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature.withSubtype(Subtype.WARRIOR), creature),
                then = Effects.Pipeline {
                    val equipment = gather(
                        filter = GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT),
                        player = Player.You,
                        includeAttachments = true
                    )
                    val chosen = chooseUpTo(
                        count = 1,
                        from = equipment,
                        useTargetingUI = true,
                        prompt = "You may attach an Equipment you control to the Warrior",
                        selectedLabel = "Attach"
                    )
                    run(
                        Effects.AttachTargetEquipmentToCreature(
                            equipmentTarget = chosen.asTarget,
                            creatureTarget = creature
                        )
                    )
                }
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "35"
        artist = "Zezhou Chen"
        flavorText = "\"If you practice something often enough, it becomes part of you. That's why we " +
            "train, and that's why we survive.\"\n—Tasha, Graypelt veteran"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78b82476-4320-4719-9785-af8d68503234.jpg?1783929407"
    }
}
