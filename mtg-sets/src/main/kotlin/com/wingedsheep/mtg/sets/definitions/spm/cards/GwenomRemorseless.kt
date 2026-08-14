package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayFromTopWithAlternativeCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gwenom, Remorseless (Marvel's Spider-Man, #56)
 * {3}{B}{B}
 * Legendary Creature — Symbiote Spider Hero
 * 4/4
 *
 * Deathtouch, lifelink
 * Whenever Gwenom attacks, until end of turn, you may look at the top card of your library any time
 * and you may play cards from the top of your library. If you cast a spell this way, pay life equal
 * to its mana value rather than pay its mana cost.
 *
 * The attack trigger grants two statics to its controller until end of turn (Duration.EndOfTurn,
 * dropped at cleanup): [LookAtTopOfLibrary] (see the top card) and [PlayFromTopWithAlternativeCost]
 * with `withoutPayingManaCost = true` + `PayLifeEqualToManaValueOfSpell` — a Bolas's-Citadel-style
 * "play from the top, pay life = mana value" permission. The top-of-library cast path scans
 * `grantedStaticAbilities` for the permission (via `CastPermissionUtils.playFromTopAlternativeCost` /
 * `CastZoneResolver`), waives the spell's mana, and charges life equal to its mana value.
 */
val GwenomRemorseless = card("Gwenom, Remorseless") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Symbiote Spider Hero"
    power = 4
    toughness = 4
    oracleText = "Deathtouch, lifelink\n" +
        "Whenever Gwenom attacks, until end of turn, you may look at the top card of your library " +
        "any time and you may play cards from the top of your library. If you cast a spell this " +
        "way, pay life equal to its mana value rather than pay its mana cost."

    keywords(Keyword.DEATHTOUCH, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.Composite(
            Effects.GrantStaticAbility(
                ability = PlayFromTopWithAlternativeCost(
                    withoutPayingManaCost = true,
                    additionalCost = Costs.additional.PayLifeEqualToManaValueOfSpell,
                ),
                target = EffectTarget.Self,
                duration = Duration.EndOfTurn,
            ),
            Effects.GrantStaticAbility(
                ability = LookAtTopOfLibrary,
                target = EffectTarget.Self,
                duration = Duration.EndOfTurn,
            ),
        )
        description = "Whenever Gwenom attacks, until end of turn, you may look at the top card of " +
            "your library any time and you may play cards from the top of your library. If you " +
            "cast a spell this way, pay life equal to its mana value rather than pay its mana cost."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "56"
        artist = "Lordigan"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46b6cc5d-7a37-4e8b-a1a5-9a573056610c.jpg?1783905344"
    }
}
