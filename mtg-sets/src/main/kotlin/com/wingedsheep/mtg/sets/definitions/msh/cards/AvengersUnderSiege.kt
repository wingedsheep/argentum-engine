package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Avengers: Under Siege — Marvel Super Heroes #205
 * {2}{B}{R} · Enchantment — Saga
 *
 * I — Create two 2/1 black Villain creature tokens with menace.
 * II — This Saga deals 2 damage to each non-Villain creature and each opponent.
 * III — Create a Treasure token for each Villain you control.
 *
 * Modeling notes:
 *  - Chapter I is the set's standard Villain token (the same 2/1 black menace token
 *    Agents of Hydra / Madame Hydra / Hire a Crew create), just with `count = 2`.
 *  - Chapter II's sweep excludes Villains via `GameObjectFilter.Creature.notSubtype(VILLAIN)`,
 *    which the group iteration reads off projected state — so a creature that has *become* a
 *    Villain (or lost the type) is judged as it currently is, and the two tokens chapter I just
 *    made are spared. It hits your own non-Villains too ("each non-Villain creature"). The
 *    each-opponent half is a separate [Effects.DealDamage] to [Player.EachOpponent]; the Saga is
 *    the source of every point of that damage.
 *  - Chapter III's "for each Villain you control" counts permanents of any card type with the
 *    Villain subtype (the set has artifact-creature and enchantment Villains), not just
 *    creatures — hence `GameObjectFilter.Any.withSubtype(VILLAIN)` rather than `Creature`.
 *    The Treasures themselves come from the engine's predefined Treasure token.
 */
val AvengersUnderSiege = card("Avengers: Under Siege") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Create two 2/1 black Villain creature tokens with menace.\n" +
        "II — This Saga deals 2 damage to each non-Villain creature and each opponent.\n" +
        "III — Create a Treasure token for each Villain you control."

    // I — Create two 2/1 black Villain creature tokens with menace.
    sagaChapter(1) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf(Subtype.VILLAIN.value),
            keywords = setOf(Keyword.MENACE),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/4/a/4a51b6a0-9a54-4f01-b959-0a28c15d103f.jpg?1783902804",
        )
    }

    // II — This Saga deals 2 damage to each non-Villain creature and each opponent.
    sagaChapter(2) {
        effect = Patterns.Group.dealDamageToAll(
            2,
            GroupFilter(GameObjectFilter.Creature.notSubtype(Subtype.VILLAIN)),
        ) then Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    // III — Create a Treasure token for each Villain you control.
    sagaChapter(3) {
        effect = Effects.CreateTreasure(
            count = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Any.withSubtype(Subtype.VILLAIN),
            ).count()
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "205"
        artist = "Serena Malyon"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c379a3f-bde1-4dc1-9843-afcb5f40792f.jpg?1783902909"
    }
}
