package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Eradicator Valkyrie — Kaldheim #94 (canonical printing)
 * {2}{B}{B} · Creature — Angel Berserker · 4/3
 *
 * Flying, lifelink, hexproof from planeswalkers
 * Boast — {1}{B}, Sacrifice a creature: Each opponent sacrifices a creature or planeswalker.
 *
 * "Hexproof from planeswalkers" (CR 702.11d) is [KeywordAbility.Hexproof] with a
 * card-type scope: abilities an opponent controls whose source is a planeswalker can't
 * target it. "Sacrifice a creature" may sacrifice the Valkyrie itself.
 */
val EradicatorValkyrie = card("Eradicator Valkyrie") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Angel Berserker"
    power = 4
    toughness = 3
    oracleText = "Flying, lifelink, hexproof from planeswalkers\n" +
        "Boast — {1}{B}, Sacrifice a creature: Each opponent sacrifices a creature or planeswalker. " +
        "(Activate only if this creature attacked this turn and only once each turn.)"

    keywords(Keyword.FLYING, Keyword.LIFELINK)
    keywordAbility(KeywordAbility.Hexproof(ProtectionScope.CardType("Planeswalker")))

    activatedAbility {
        isBoast = true
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.Sacrifice(GameObjectFilter.Creature))
        effect = Effects.Sacrifice(
            GameObjectFilter.CreatureOrPlaneswalker,
            1,
            EffectTarget.PlayerRef(Player.EachOpponent)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "94"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d00edda3-ddbc-44ee-a14b-47a701445bb0.jpg?1783928247"
        ruling("2021-02-05", "Eradicator Valkyrie can't be the target of abilities whose source is a planeswalker an opponent controls.")
        ruling("2021-02-05", "As the boast ability resolves, first the next opponent in turn order chooses a creature or planeswalker they control. Then each other opponent in turn order chooses a creature or planeswalker they control, knowing the choices made before their choice. Then all chosen creatures and planeswalkers are sacrificed at the same time.")
        ruling("2021-02-05", "A boast ability can be activated at any point after the creature with that ability has been declared as an attacker. This can be before blockers are declared, after blockers are declared but before combat damage is dealt, during combat after combat damage is dealt, during the postcombat main phase, during the end step, or, in some unusual cases, during the cleanup step.")
        ruling("2021-02-05", "If it's not your turn and you gain control of a creature with a boast ability after that creature attacked, you can activate that creature's boast ability if it hasn't been activated yet that turn.")
        ruling("2021-02-05", "If a creature with a boast ability is put onto the battlefield attacking, it was never declared as an attacker. Its boast ability can't be activated that turn.")
        ruling("2021-02-05", "If an effect adds additional combat phases to a turn and a creature with a boast ability attacks more than once during that turn, its boast ability can still be activated only once.")
    }
}
