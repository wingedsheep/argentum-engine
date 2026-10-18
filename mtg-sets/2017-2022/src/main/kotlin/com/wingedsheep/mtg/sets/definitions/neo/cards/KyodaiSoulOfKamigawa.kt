package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kyodai, Soul of Kamigawa — Kamigawa: Neon Dynasty #23 (canonical printing)
 * {3}{W} · Legendary Creature — Dragon Spirit · 3/3
 *
 * Flash
 * Flying
 * When Kyodai enters, another target permanent gains indestructible for as long as you control Kyodai.
 * {W}{U}{B}{R}{G}: Kyodai gets +5/+5 until end of turn.
 *
 * The indestructible grant is a floating effect with `Duration.WhileYouControlSource` — the same
 * gate Vodalian Mindsinger's steal uses: it ends for good once Kyodai leaves the battlefield or
 * changes controller. Color identity is WUBRG because of the activated ability's cost.
 */
val KyodaiSoulOfKamigawa = card("Kyodai, Soul of Kamigawa") {
    manaCost = "{3}{W}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — Dragon Spirit"
    power = 3
    toughness = 3
    oracleText = "Flash\nFlying\n" +
        "When Kyodai enters, another target permanent gains indestructible for as long as you control Kyodai.\n" +
        "{W}{U}{B}{R}{G}: Kyodai gets +5/+5 until end of turn."

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Permanent.other())
        effect = Effects.GrantKeyword(
            Keyword.INDESTRUCTIBLE,
            t,
            Duration.WhileYouControlSource("Kyodai, Soul of Kamigawa")
        )
        description = "When Kyodai enters, another target permanent gains indestructible for as long as you control Kyodai."
    }

    activatedAbility {
        cost = Costs.Mana("{W}{U}{B}{R}{G}")
        effect = Effects.ModifyStats(5, 5, EffectTarget.Self)
        description = "{W}{U}{B}{R}{G}: Kyodai gets +5/+5 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "23"
        artist = "Daniel Zrom"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cbbfd21e-96f1-4e5b-8546-73bb5a887b98.jpg?1783923918"
    }
}
