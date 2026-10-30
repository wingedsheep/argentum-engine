package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kiora Bests the Sea God
 * {5}{U}{U}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Create an 8/8 blue Kraken creature token with hexproof.
 * II — Tap all nonland permanents target opponent controls. They don't untap during their
 * controller's next untap step.
 * III — Gain control of target permanent an opponent controls. Untap it.
 *
 * Chapter II walks every nonland permanent the targeted opponent controls (the player is the
 * target, not the permanents — hexproof ones are tapped too), tapping each and pinning it with
 * the Press for Answers "doesn't untap during its controller's next untap step" grant. Already
 * tapped permanents still get the grant (ruling), and the grant outlives the Saga. Chapter III's
 * control change is indefinite.
 */
private const val KRAKEN_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/c/3/c3365aea-823d-44a9-94d3-ed2a31d85f34.jpg?1783931458"

val KioraBestsTheSeaGod = card("Kiora Bests the Sea God") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Create an 8/8 blue Kraken creature token with hexproof.\n" +
        "II — Tap all nonland permanents target opponent controls. They don't untap during their " +
        "controller's next untap step.\n" +
        "III — Gain control of target permanent an opponent controls. Untap it."

    sagaChapter(1) {
        effect = Effects.CreateToken(
            power = 8,
            toughness = 8,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Kraken"),
            keywords = setOf(Keyword.HEXPROOF),
            imageUri = KRAKEN_TOKEN_IMAGE,
        )
    }

    sagaChapter(2) {
        val opponent = target(Targets.Opponent)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.NonlandPermanent.targetPlayerControls(opponent)),
            Effects.Tap(EffectTarget.IterationEntity) then
                Effects.GrantKeyword(
                    AbilityFlag.DOESNT_UNTAP,
                    EffectTarget.IterationEntity,
                    Duration.UntilAfterAffectedControllersNextUntap(),
                ),
        )
    }

    sagaChapter(3) {
        val permanent = target(TargetFilter.PermanentOpponentControls)
        effect = Effects.GainControl(permanent) then Effects.Untap(permanent)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "52"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b604451-4f6c-4cfa-8d3a-01f18d01d88f.jpg?1783931583"
        ruling(
            "2020-01-24",
            "Nonland permanents target opponent controls that are already tapped when the second chapter " +
                "ability resolves won't be tapped again, but they won't untap during their controller's next " +
                "untap step.",
        )
        ruling(
            "2020-01-24",
            "The second chapter ability keeps the permanents tapped even if Kiora Bests the Sea God leaves " +
                "the battlefield before that player's next untap step.",
        )
        ruling(
            "2020-01-24",
            "Any nonland permanents that come under the target player's control after the second chapter " +
                "ability resolves will untap as normal.",
        )
        ruling(
            "2020-01-24",
            "The second chapter ability targets the player but not their permanents. Permanents with " +
                "hexproof may be tapped this way.",
        )
        ruling(
            "2020-01-24",
            "The control-change effect of the third chapter ability lasts indefinitely. It doesn't wear off " +
                "during the cleanup step. In a multiplayer game, it does expire if you leave the game.",
        )
    }
}
