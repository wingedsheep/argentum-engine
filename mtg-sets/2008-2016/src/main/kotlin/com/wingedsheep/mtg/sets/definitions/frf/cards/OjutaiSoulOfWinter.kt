package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ojutai, Soul of Winter — Fate Reforged #156 (canonical printing)
 * {5}{W}{U}
 * Legendary Creature — Dragon
 * 5/6
 * Flying, vigilance
 * Whenever a Dragon you control attacks, tap target nonland permanent an opponent controls.
 * That permanent doesn't untap during its controller's next untap step.
 *
 * A per-attacker ANY-bound attack trigger (Dromoka, the Eternal's shape), one instance per
 * attacking Dragon. The rider is the Tamiyo, the Moon Sage / Crippling Chill pair: tap, then
 * [AbilityFlag.DOESNT_UNTAP] for [Duration.UntilAfterAffectedControllersNextUntap], which keys off
 * the permanent's current controller — the ruling that it tracks the permanent, not its controller.
 * Already-tapped permanents are legal targets.
 */
val OjutaiSoulOfWinter = card("Ojutai, Soul of Winter") {
    manaCost = "{5}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Dragon"
    power = 5
    toughness = 6
    oracleText = "Flying, vigilance\n" +
        "Whenever a Dragon you control attacks, tap target nonland permanent an opponent controls. " +
        "That permanent doesn't untap during its controller's next untap step."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withSubtype("Dragon").youControl()).attacks()
        val permanent = target(TargetFilter.NonlandPermanentOpponentControls)
        effect = Effects.Tap(permanent) then Effects.GrantKeyword(
            AbilityFlag.DOESNT_UNTAP,
            permanent,
            Duration.UntilAfterAffectedControllersNextUntap()
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "156"
        artist = "Chase Stone"
        flavorText = "\"When I battled Ojutai, I realized my learning had only begun.\"\n—Shu Yun, the Silent Tempest"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67d470cd-9ff9-49d8-b66a-331cb6cf8257.jpg?1783938672"
        ruling("2014-11-24", "The triggered ability can target a nonland permanent that's already tapped. That permanent won't untap during its controller's next untap step.")
        ruling("2014-11-24", "The ability tracks the permanent, but not its controller. If the permanent changes controllers before its first controller's next untap step, then it won't untap during its new controller's next untap step.")
        ruling("2014-11-24", "The ability only applies during the controller's next untap step, even if the permanent has been the target of Ojutai's triggered ability more than once. The permanent won't stay tapped through multiple untap steps.")
    }
}
