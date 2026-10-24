package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Extus, Oriq Overlord // Awaken the Blood Avatar — Strixhaven: School of Mages #149
 *
 * Modal double-faced card (CR 712). Cast either face from hand, never both.
 *
 * Front — Extus, Oriq Overlord ({1}{W}{B}{B}, Legendary Creature — Human Warlock, 2/4):
 *   Double strike. Magecraft — return target nonlegendary creature card from your graveyard to
 *   your hand (not optional — ruling).
 *
 * Back — Awaken the Blood Avatar ({6}{B}{R}, Sorcery):
 *   "As an additional cost to cast this spell, you may sacrifice any number of creatures. This
 *   spell costs {2} less to cast for each creature sacrificed this way." Torgaar's cost on the
 *   back face; generic reduction floors at {B}{R} (ruling). Then each opponent sacrifices a
 *   creature of their choice and you create a 3/6 black and red Avatar with haste and "Whenever
 *   this token attacks, it deals 3 damage to each opponent."
 */
val ExtusOriqOverlord = card("Extus, Oriq Overlord") {
    manaCost = "{1}{W}{B}{B}"
    colorIdentity = "WBR"
    typeLine = "Legendary Creature — Human Warlock"
    oracleText = "Double strike\n" +
        "Magecraft — Whenever you cast or copy an instant or sorcery spell, return target " +
        "nonlegendary creature card from your graveyard to your hand."
    power = 2
    toughness = 4

    keywords(Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard.nonlegendary())
        effect = Effects.ReturnToHand(creatureCard)
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, return " +
            "target nonlegendary creature card from your graveyard to your hand."
    }

    modalBack("Awaken the Blood Avatar") {
        manaCost = "{6}{B}{R}"
        typeLine = "Sorcery"
        oracleText = "As an additional cost to cast this spell, you may sacrifice any number of " +
            "creatures. This spell costs {2} less to cast for each creature sacrificed this way.\n" +
            "Each opponent sacrifices a creature of their choice. Create a 3/6 black and red " +
            "Avatar creature token with haste and \"Whenever this token attacks, it deals 3 " +
            "damage to each opponent.\""
        imageUri = "https://cards.scryfall.io/normal/back/b/a/ba09360a-067e-48a5-bdc5-a19fd066a785.jpg?1783927337"
        additionalCost(Costs.additional.SacrificeAnyNumberForCostReduction(GameObjectFilter.Creature, reductionPerUnit = 2))
        spell {
            effect = Effects.Sacrifice(
                GameObjectFilter.Creature,
                target = EffectTarget.PlayerRef(Player.EachOpponent),
            ) then Effects.CreateToken(
                power = 3,
                toughness = 6,
                colors = setOf(Color.BLACK, Color.RED),
                creatureTypes = setOf("Avatar"),
                keywords = setOf(Keyword.HASTE),
                triggeredAbilities = listOf(
                    TriggeredAbility.create(
                        trigger = Triggers.self.attacks(),
                        effect = Effects.DealDamage(3, EffectTarget.PlayerRef(Player.EachOpponent)),
                    )
                ),
                imageUri = "https://cards.scryfall.io/normal/front/9/4/94a50acd-ac2d-47bf-b331-0bcf5edd9c75.jpg?1783927193",
            )
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "149"
        artist = "Chase Stone & Kekai Kotaki"
        flavorText = "\"Join me for the dawn of a new age.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba09360a-067e-48a5-bdc5-a19fd066a785.jpg?1783927337"
        ruling("2021-04-16", "Returning a nonlegendary creature card is not optional. If there is at least one in your graveyard, you must target it with the magecraft ability.")
        ruling("2021-04-16", "Awaken the Blood Avatar's first ability can't reduce its cost below {B}{R}. However, you can sacrifice any number of creatures, even if they won't reduce its cost any further.")
        ruling("2021-04-16", "Include any additional costs to cast Awaken the Blood Avatar before applying its own cost reduction. For example, if Extus is your commander, and an additional cost of {6} applies from the \"commander tax,\" you may cast Awaken the Blood Avatar by sacrificing four creatures and paying {4}{B}{R}.")
        ruling("2021-04-16", "As Awaken the Blood Avatar resolves, first the next opponent in turn order (or, if it's an opponent's turn, the opponent whose turn it is) chooses a creature they control, then each other opponent in turn order does the same knowing the choices made before them. Then all the chosen creatures are sacrificed at the same time.")
        ruling("2021-04-16", "The Blood Avatar will be awakened even if no player has creatures to sacrifice when it resolves.")
    }
}
