package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Lasyd Prowler — Tarkir: Dragonstorm #149
 * {2}{G}{G} · Creature — Snake Ranger · 5/5
 *
 * When this creature enters, you may mill cards equal to the number of lands you control.
 * Renew — {1}{G}, Exile this card from your graveyard: Put X +1/+1 counters on target
 * creature, where X is the number of land cards in your graveyard. Activate only as a sorcery.
 *
 * The Renew ability counts land cards in the graveyard at resolution. Lasyd Prowler itself
 * is exiled as a cost (it is not a land), so it never contributes to X.
 */
val LasydProwler = card("Lasyd Prowler") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake Ranger"
    power = 5
    toughness = 5
    oracleText = "When this creature enters, you may mill cards equal to the number of lands you control.\n" +
        "Renew — {1}{G}, Exile this card from your graveyard: Put X +1/+1 counters on target creature, " +
        "where X is the number of land cards in your graveyard. Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            EffectPatterns.mill(DynamicAmount.Count(Player.You, Zone.BATTLEFIELD, Filters.Land)),
            descriptionOverride = "You may mill cards equal to the number of lands you control."
        )
        description = "When this creature enters, you may mill cards equal to the number of lands you control."
    }

    renew("{1}{G}") {
        effect = Effects.AddDynamicCounters(
            Counters.PLUS_ONE_PLUS_ONE,
            DynamicAmount.Count(Player.You, Zone.GRAVEYARD, Filters.Land),
            target("creature", Targets.Creature)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "149"
        artist = "Anna Pavleeva"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7f5c8ef-8e9e-4264-a7d2-126a30a5d341.jpg?1743204564"
    }
}
