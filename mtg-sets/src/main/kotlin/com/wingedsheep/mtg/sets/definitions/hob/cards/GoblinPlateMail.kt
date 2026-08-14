package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Goblin Plate Mail
 * {1}{B/R}
 * Artifact — Equipment
 *
 * When this Equipment enters, amass Goblins 1, then attach this Equipment to the amassed Army.
 * Equipped creature gets +1/+0 and has menace.
 * Equip {4}
 *
 * Modeling notes:
 *  - "Then attach … to the amassed Army" is not a target — it is the Army the Amass step
 *    chose (CR 701.47c), whether or not counters actually landed on it. The engine writes
 *    that id into `EntityReference.AmassedArmy.STORAGE_KEY` on the resolution pipeline, so
 *    the follow-up attach reads it back as `EffectTarget.PipelineTarget(STORAGE_KEY)`. That
 *    slot survives the multi-Army choice continuation, so attaching still lands on the right
 *    Army when the player had to pick between several.
 *  - A plain [Effects.Composite] is right here (not a reflexive trigger): "amass … , then
 *    attach" is one resolution with no second trigger and nothing to respond to in between —
 *    unlike Foray of Orcs' "When you do".
 */
val GoblinPlateMail = card("Goblin Plate Mail") {
    manaCost = "{1}{B/R}"
    colorIdentity = "BR"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, amass Goblins 1, then attach this Equipment to " +
        "the amassed Army. (To amass Goblins 1, put a +1/+1 counter on an Army you control. " +
        "It's also a Goblin. If you don't control an Army, create a 0/0 black Goblin Army " +
        "creature token first.)\n" +
        "Equipped creature gets +1/+0 and has menace.\n" +
        "Equip {4}"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            Effects.Amass(1, "Goblin"),
            Effects.AttachEquipment(
                EffectTarget.PipelineTarget(EntityReference.AmassedArmy.STORAGE_KEY)
            ),
        )
        description = "When this Equipment enters, amass Goblins 1, then attach this Equipment " +
            "to the amassed Army."
    }

    staticAbility {
        ability = ModifyStats(+1, +0, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.MENACE, Filters.EquippedCreature)
    }

    equipAbility("{4}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "157"
        artist = "Miklós Ligeti"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb982607-da37-4894-91a5-cf6307d4d703.jpg?1785323293"
    }
}
