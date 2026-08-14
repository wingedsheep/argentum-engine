package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect

/**
 * Spider-Woman, Secret Agent — Marvel Super Heroes #229
 * {3}{W/U} · Legendary Creature — Spider Human Spy Hero · 1/4
 *
 * Flash
 * When Spider-Woman enters, tap target creature an opponent controls. That creature can't
 * become untapped for as long as you control Spider-Woman.
 *
 * Modeling notes:
 *  - The Ty Lee, Chi Blocker shape: one mandatory target shared by the tap and the lock, so both
 *    halves are bound to the same `BoundVariable` rather than re-resolving a positional target.
 *    Unlike Ty Lee, this target is *not* "up to one" — it's a required "target creature an
 *    opponent controls", so the ETB is removed from the stack with no effect if that creature is
 *    gone on resolution (CR 608.2b).
 *  - "can't become untapped" is the strong flag [AbilityFlag.CANT_BECOME_UNTAPPED], not
 *    [AbilityFlag.DOESNT_UNTAP]: it blocks untap *effects* too, not just the controller's untap
 *    step. Ty Lee's printed wording ("doesn't untap during its controller's untap step") is the
 *    weaker one — the two are easy to swap and resolve differently against a Twiddle.
 *  - "for as long as you control Spider-Woman" is [Duration.WhileYouControlSource], not
 *    [Duration.WhileSourceOnBattlefield]: a Threaten-style steal of Spider-Woman ends the lock
 *    immediately (CR 611.2b), and the effect does not restart if she comes back to you.
 */
val SpiderWomanSecretAgent = card("Spider-Woman, Secret Agent") {
    manaCost = "{3}{W/U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Spider Human Spy Hero"
    oracleText = "Flash\n" +
        "When Spider-Woman enters, tap target creature an opponent controls. That creature " +
        "can't become untapped for as long as you control Spider-Woman."
    power = 1
    toughness = 4

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("target creature an opponent controls", Targets.CreatureOpponentControls)
        effect = Effects.Tap(creature) then
            GrantKeywordEffect(
                AbilityFlag.CANT_BECOME_UNTAPPED.name,
                creature,
                Duration.WhileYouControlSource("Spider-Woman"),
            )
        description = "When Spider-Woman enters, tap target creature an opponent controls. That " +
            "creature can't become untapped for as long as you control Spider-Woman."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "229"
        artist = "Pauline Voss"
        flavorText = "\"I'm not friendly, and this isn't my neighborhood.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0325cb5-4c43-418a-8f1b-cf5bf29e74d7.jpg?1783902897"
    }
}
