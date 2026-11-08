package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thurid, Mare of Destiny — Foundations Jumpstart #33 (canonical printing)
 * {2}{W}{W} · Legendary Creature — Pegasus · 2/4
 *
 * Flying, lifelink
 * Whenever you cast a Pegasus, Unicorn, or Horse creature spell, copy it. (The copy becomes a token.)
 * Other Pegasi, Unicorns, and Horses you control get +1/+1.
 *
 * The copy trigger is Tawnos, the Toymaker's shape without the "may" or the exception: the cast
 * trigger filters creature spells (so noncreature Kindred spells of those types don't trigger it,
 * per the ruling) and copies the triggering spell, which resolves into a token (CR 707.10f). The
 * anthem is one lord static over any of the three subtypes, so a creature that is several of them
 * gets +1/+1 only once.
 */
val ThuridMareOfDestiny = card("Thurid, Mare of Destiny") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Pegasus"
    power = 2
    toughness = 4
    oracleText = "Flying, lifelink\n" +
        "Whenever you cast a Pegasus, Unicorn, or Horse creature spell, copy it. (The copy becomes a token.)\n" +
        "Other Pegasi, Unicorns, and Horses you control get +1/+1."

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature.withAnySubtype("Pegasus", "Unicorn", "Horse"))
        effect = Effects.CopyTargetSpell(EffectTarget.TriggeringEntity, mayChooseNewTargets = false)
    }

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withAnyOfSubtypes(
                    listOf(Subtype.PEGASUS, Subtype.UNICORN, Subtype.HORSE)
                ).youControl(),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "33"
        artist = "Ishikawa Kenta"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aad65c60-b9e8-4a8f-92e8-3dff63232102.jpg?1783908860"
        ruling("2024-11-08", "Thurid's triggered ability and the copy it creates will resolve before the spell that caused it to trigger. They resolve even if that spell is countered or otherwise leaves the stack without resolving.")
        ruling("2024-11-08", "The token that a resolving copy of a permanent spell becomes isn't \"created.\" Abilities that refer to a token being created won't interact with the copy resolving.")
        ruling("2024-11-08", "Thurid's triggered ability will copy only creature spells, not noncreature kindred spells, even if those spells have the Pegasus, Unicorn, or Horse types.")
    }
}
