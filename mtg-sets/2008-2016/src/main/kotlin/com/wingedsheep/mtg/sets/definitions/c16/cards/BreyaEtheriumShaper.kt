package com.wingedsheep.mtg.sets.definitions.c16.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Breya, Etherium Shaper
 * {W}{U}{B}{R}
 * Legendary Artifact Creature — Human
 * When Breya enters, create two 1/1 blue Thopter artifact creature tokens with flying.
 * {2}, Sacrifice two artifacts: Choose one —
 * • Breya deals 3 damage to target player or planeswalker.
 * • Target creature gets -4/-4 until end of turn.
 * • You gain 5 life.
 *
 * A modal *ability*, so `countsAsModalSpell = false`. Breya is itself an artifact and may be
 * one of the two sacrificed; the damage mode then uses Breya's last-known information.
 */
val BreyaEtheriumShaper = card("Breya, Etherium Shaper") {
    manaCost = "{W}{U}{B}{R}"
    colorIdentity = "WUBR"
    typeLine = "Legendary Artifact Creature — Human"
    power = 4
    toughness = 4
    oracleText = "When Breya enters, create two 1/1 blue Thopter artifact creature tokens with flying.\n" +
        "{2}, Sacrifice two artifacts: Choose one —\n" +
        "• Breya deals 3 damage to target player or planeswalker.\n" +
        "• Target creature gets -4/-4 until end of turn.\n" +
        "• You gain 5 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            count = 2,
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/6/c/6c9a9020-2bf0-4bdd-94a2-545304226a53.jpg?1783937013"
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.SacrificeMultiple(2, GameObjectFilter.Artifact))
        effect = ModalEffect.chooseOne(
            mode("Breya deals 3 damage to target player or planeswalker") {
                val victim = target(Targets.PlayerOrPlaneswalker)
                effect = Effects.DealDamage(3, victim)
            },
            mode("Target creature gets -4/-4 until end of turn") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(-4, -4, creature)
            },
            Mode.noTarget(Effects.GainLife(5), "You gain 5 life"),
            countsAsModalSpell = false
        )
        description = "{2}, Sacrifice two artifacts: Choose one — Breya deals 3 damage to target " +
            "player or planeswalker; or target creature gets -4/-4 until end of turn; or you gain 5 life."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "29"
        artist = "Clint Cearley"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b585ce8-77a3-4755-8aa6-aee83724bbab.jpg?1783937085"
    }
}
