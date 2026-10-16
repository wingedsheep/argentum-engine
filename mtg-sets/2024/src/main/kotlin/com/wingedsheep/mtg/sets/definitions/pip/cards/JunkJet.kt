package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Junk Jet — Fallout #60
 * {1}{R} · Artifact — Equipment
 *
 * When this Equipment enters, create a Junk token.
 * {3}, Sacrifice another artifact: Double equipped creature's power until end of turn.
 * Equip {1}
 *
 * "Double its power" is the standard +X/+0 where X is the equipped creature's power as the
 * effect begins to apply (read at resolution), the same shape as Bulk Up — a negative power
 * therefore yields -X/-0, per the ruling. [Costs.SacrificeAnother] keeps Junk Jet itself out of
 * its own cost. With nothing equipped the effect has no creature and does nothing.
 */
val JunkJet = card("Junk Jet") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, create a Junk token. (It's an artifact with \"{T}, Sacrifice this token: Exile the top card of your library. You may play that card this turn. Activate only as a sorcery.\")\n" +
        "{3}, Sacrifice another artifact: Double equipped creature's power until end of turn.\n" +
        "Equip {1}"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateJunk()
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.SacrificeAnother(GameObjectFilter.Artifact))
        effect = Effects.ModifyStats(
            power = DynamicAmounts.powerOf(EffectTarget.EquippedCreature),
            toughness = DynamicAmounts.fixed(0),
            target = EffectTarget.EquippedCreature
        )
        description = "{3}, Sacrifice another artifact: Double equipped creature's power until end of turn."
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "60"
        artist = "Billy Christian"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2397c9b8-3974-48b8-94bd-f05f2e20750c.jpg?1783912403"
        ruling("2024-03-08", "If an effect instructs you to \"double\" a creature's power, that creature gets +X/+0, where X is its power as that effect begins to apply.")
        ruling("2024-03-08", "If a creature's power is less than 0 when it's doubled, instead that creature gets -X/-0, where X is how much less than 0 its power is.")
        ruling("2024-03-08", "If you \"double\" a creature's power more than once in a turn, each doubling effect applies independently.")
        ruling("2024-03-08", "You can't sacrifice a Junk token to pay multiple costs. For example, you can't sacrifice a Junk token to activate its own ability and also to activate the second ability of Junk Jet.")
    }
}
