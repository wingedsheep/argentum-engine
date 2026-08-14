package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Woodland Weavemaster
 * {1}{G}
 * Creature — Elf Druid
 * 1/2
 *
 * Vigilance
 * Whenever another Elf you control enters, this creature gets +1/+1 until end of turn.
 * {T}: Add X mana of any one color, where X is this creature's power. Spend this mana only
 * to cast Elf spells and activate abilities of Elf sources.
 *
 * Modeling notes:
 *  - "Another Elf **you control**" is an OTHER-bound enters trigger over
 *    `Elf.youControl()` — the OTHER binding is what keeps the Weavemaster's own ETB from
 *    pumping itself. The filter is "Elf", not "Elf creature": a noncreature Elf permanent
 *    would still count, and the projected type is what the filter reads.
 *  - The mana ability's amount is [DynamicAmounts.sourcePower], read at resolution, so the
 *    pumps this creature has already taken this turn (and any counters on it) are included.
 *    Power 0 or less simply adds no mana.
 *  - "Elf spells **and** abilities of Elf sources" is
 *    [ManaRestriction.SubtypeSpellsOrAbilitiesOnly] with `creatureOnly = false` — the
 *    Unclaimed Territory shape, where activations of Elf sources also qualify — rather than
 *    the Cavern of Souls creature-spells-only shape.
 */
val WoodlandWeavemaster = card("Woodland Weavemaster") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 1
    toughness = 2
    oracleText = "Vigilance\n" +
        "Whenever another Elf you control enters, this creature gets +1/+1 until end of turn.\n" +
        "{T}: Add X mana of any one color, where X is this creature's power. Spend this mana " +
        "only to cast Elf spells and activate abilities of Elf sources."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Any.withSubtype(Subtype.ELF).youControl(),
            binding = TriggerBinding.OTHER,
        )
        effect = Effects.ModifyStats(+1, +1, EffectTarget.Self)
        description = "Whenever another Elf you control enters, this creature gets +1/+1 until " +
            "end of turn."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(
            amount = DynamicAmounts.sourcePower(),
            restriction = ManaRestriction.SubtypeSpellsOrAbilitiesOnly("Elf", creatureOnly = false),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "Add X mana of any one color, where X is this creature's power. Spend this " +
            "mana only to cast Elf spells and activate abilities of Elf sources."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Nia Kovalevski"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe2b4bcf-56de-44d3-83af-aeb27f82c25e.jpg?1785237990"
    }
}
