package com.wingedsheep.mtg.sets.tokens

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.model.CardDefinition.Companion.doubleFacedPermanent
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Predefined token CardDefinitions.
 *
 * These are registered in the CardRegistry so that the engine can look up token abilities
 * by name (e.g., "Treasure" → its mana ability). The unified [CreatePredefinedTokenExecutor]
 * creates entities with a matching `name` field, and the engine resolves abilities via
 * `cardRegistry.getCard(name)`.
 *
 * To add a new predefined token type, define it here and add a facade method to `Effects.kt`.
 */
object PredefinedTokens {

    /**
     * Treasure token — an artifact with:
     * "{T}, Sacrifice this artifact: Add one mana of any color."
     */
    val Treasure = card("Treasure") {
        typeLine = "Artifact - Treasure"

        activatedAbility {
            cost = Costs.Composite(
                Costs.Tap,
                Costs.SacrificeSelf
            )
            effect = Effects.AddAnyColorMana(1)
            manaAbility = true
        }

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/4/8/4837a3f1-ca7f-41e5-a5d1-729c8495b0e8.jpg?1771590279"
        }
    }

    /**
     * Meteorite token — a colorless artifact (Roxanne, Starfall Savant) with:
     * "When this token enters, it deals 2 damage to any target." and
     * "{T}: Add one mana of any color."
     */
    val Meteorite = card("Meteorite") {
        typeLine = "Artifact"

        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            val anyTarget = target("any target", Targets.Any)
            effect = Effects.DealDamage(2, anyTarget, damageSource = EffectTarget.Self)
            description = "When this token enters, it deals 2 damage to any target."
        }

        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddAnyColorMana(1)
            manaAbility = true
        }

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/0/0/00b41ca9-0bf0-41fc-af65-854e602ee007.jpg?1712317015"
            artist = "Ina Wong"
        }
    }

    /**
     * Food token — an artifact with:
     * "{2}, {T}, Sacrifice this artifact: You gain 3 life."
     */
    val Food = card("Food") {
        typeLine = "Artifact - Food"

        activatedAbility {
            cost = Costs.Composite(
                Costs.Mana("{2}"),
                Costs.Tap,
                Costs.SacrificeSelf
            )
            effect = Effects.GainLife(3)
        }

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/0/d/0dce2241-e58b-41d4-b57c-9794fc8ee004.jpg?1721425221"
        }
    }

    /**
     * Blood token — an artifact with:
     * "{1}, {T}, Discard a card, Sacrifice this artifact: Draw a card."
     */
    val Blood = card("Blood") {
        typeLine = "Artifact — Blood"

        activatedAbility {
            cost = Costs.Composite(
                Costs.Mana("{1}"),
                Costs.Tap,
                Costs.DiscardCard,
                Costs.SacrificeSelf
            )
            effect = Effects.DrawCards(1)
        }

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/9/2/92a3afe7-bd9f-43f9-adc3-4819e60dc7a5.jpg?1743236443"
        }
    }

    /**
     * Clue token — an artifact with:
     * "{2}, Sacrifice this token: Draw a card."
     * Created by the Investigate keyword action ([Effects.Investigate]).
     */
    val Clue = card("Clue") {
        typeLine = "Artifact — Clue"

        activatedAbility {
            cost = Costs.Composite(
                Costs.Mana("{2}"),
                Costs.SacrificeSelf
            )
            effect = Effects.DrawCards(1)
        }

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/7/6/764a906c-8b27-4ffa-bdc3-7825c6919d3e.jpg?1712316807"
        }
    }

    /**
     * Shard token — a colorless Enchantment — Shard with:
     * "{2}, Sacrifice this enchantment: Scry 1, then draw a card."
     * Created by Niko, Light of Hope ([Effects.CreateShard]). The Clue token's enchantment cousin
     * (Scry 1 then draw, rather than just draw).
     */
    val Shard = card("Shard") {
        typeLine = "Enchantment — Shard"

        activatedAbility {
            cost = Costs.Composite(
                Costs.Mana("{2}"),
                Costs.SacrificeSelf
            )
            effect = Effects.Composite(
                Effects.Scry(1),
                Effects.DrawCards(1)
            )
        }

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/6/a/6a198942-049c-4537-b5b1-d35df32d45d5.jpg?1726236836"
        }
    }

    /**
     * Lander token — an artifact with:
     * "{2}, {T}, Sacrifice this token: Search your library for a basic land card,
     * put it onto the battlefield tapped, then shuffle."
     */
    val Lander = card("Lander") {
        typeLine = "Artifact - Lander"

        activatedAbility {
            cost = Costs.Composite(
                Costs.Mana("{2}"),
                Costs.Tap,
                Costs.SacrificeSelf
            )
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true,
                shuffleAfter = true
            )
        }

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/8/5/85ef1950-219f-401b-8ff5-914f9aaec122.jpg?1752946491"
            artist = "Jorge Jacinto"
            collectorNumber = "8"
        }
    }

    /**
     * "Just One Glass" — a named Food token created by Sekshaas, Early Sleeper.
     * Functionally identical to a Food token, but with custom name and art.
     */
    val JustOneGlass = card("Just One Glass") {
        typeLine = "Artifact - Food"

        activatedAbility {
            cost = Costs.Composite(
                Costs.Mana("{2}"),
                Costs.Tap,
                Costs.SacrificeSelf
            )
            effect = Effects.GainLife(3)
        }

        metadata {
            imageUri = "/images/custom/just-one-glass.jpeg"
        }
    }

    /**
     * Sword token — a colorless Equipment artifact with:
     * "Equipped creature gets +1/+1" and equip {2}.
     * Created by Blacksmith's Talent.
     */
    val Sword = card("Sword") {
        typeLine = "Artifact — Equipment"

        staticAbility {
            ability = ModifyStats(+1, +1, Filters.EquippedCreature)
        }

        equipAbility("{2}")

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/b/b/bb1e78e6-a9e7-48a4-9231-61fb331c5837.jpg?1721426299"
        }
    }

    /**
     * Cragflame — a legendary colorless Equipment artifact token with:
     * "Equipped creature gets +1/+1 and has vigilance, trample, and haste" and equip {2}.
     * Created by Mabel, Heir to Cragflame.
     */
    val Cragflame = card("Cragflame") {
        typeLine = "Legendary Artifact — Equipment"

        staticAbility {
            ability = ModifyStats(+1, +1, Filters.EquippedCreature)
        }

        staticAbility {
            ability = GrantKeyword(Keyword.VIGILANCE, Filters.EquippedCreature)
        }

        staticAbility {
            ability = GrantKeyword(Keyword.TRAMPLE, Filters.EquippedCreature)
        }

        staticAbility {
            ability = GrantKeyword(Keyword.HASTE, Filters.EquippedCreature)
        }

        equipAbility("{2}")

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/c/7/c76fa1c6-6000-47b2-9188-9c15b2c73f8f.jpg?1721431172"
        }
    }

    /**
     * Mutavault — a Land token with:
     * "{T}: Add {C}."
     * "{1}: This token becomes a 2/2 creature with all creature types until end of turn.
     *  It's still a land."
     * Created by Mutable Explorer.
     */
    val Mutavault = card("Mutavault") {
        typeLine = "Land — Mutavault"

        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddColorlessMana(1)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }

        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = BecomeCreatureEffect(
                target = EffectTarget.Self,
                power = DynamicAmount.Fixed(2),
                toughness = DynamicAmount.Fixed(2),
                keywords = setOf(Keyword.CHANGELING)
            )
        }

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/3/d/3d2f5d31-a1c6-465f-b518-b40acdfab8aa.jpg?1767955820"
        }
    }

    /**
     * Sorcerer Role — Enchantment — Aura Role token created by Spellbook Vendor and others.
     * "Enchanted creature gets +1/+1. Whenever enchanted creature attacks, scry 1."
     */
    val SorcererRole = card("Sorcerer Role") {
        typeLine = "Enchantment — Aura Role"
        oracleText = "Enchant creature\nEnchanted creature gets +1/+1 and has \"Whenever this creature attacks, scry 1.\""

        auraTarget = Targets.Creature

        staticAbility {
            ability = ModifyStats(+1, +1, Filters.EnchantedCreature)
        }

        triggeredAbility {
            trigger = Triggers.attacks(binding = TriggerBinding.ATTACHED)
            effect = Patterns.Library.scry(1)
        }

        metadata {
            // "Monster // Sorcerer" flip token: the single image shows Monster upright; Sorcerer is
            // the bottom face and reads upside-down, so rotate 180° to display it correctly.
            imageUri = "https://cards.scryfall.io/normal/front/6/b/6b8a810b-8538-41c3-a792-dbd1a1845faa.jpg?1694737457"
            imageRotation = 180
            artist = "Rovina Cai"
        }
    }

    /**
     * Monster Role — Enchantment — Aura Role token (Wilds of Eldraine).
     * "Enchanted creature gets +1/+1 and has trample."
     */
    val MonsterRole = card("Monster Role") {
        typeLine = "Enchantment — Aura Role"
        oracleText = "Enchant creature\nEnchanted creature gets +1/+1 and has trample."

        auraTarget = Targets.Creature

        staticAbility {
            ability = ModifyStats(+1, +1, Filters.EnchantedCreature)
        }

        staticAbility {
            ability = GrantKeyword(Keyword.TRAMPLE, Filters.EnchantedCreature)
        }

        metadata {
            // "Monster // Sorcerer" flip token: Monster is the upright (front) face — no rotation.
            imageUri = "https://cards.scryfall.io/normal/front/6/b/6b8a810b-8538-41c3-a792-dbd1a1845faa.jpg?1694737457"
            artist = "Rovina Cai"
        }
    }

    /**
     * Royal Role — Enchantment — Aura Role token (Wilds of Eldraine).
     * "Enchanted creature gets +1/+1 and has ward {1}."
     */
    val RoyalRole = card("Royal Role") {
        typeLine = "Enchantment — Aura Role"
        oracleText = "Enchant creature\nEnchanted creature gets +1/+1 and has ward {1}."

        auraTarget = Targets.Creature

        staticAbility {
            ability = ModifyStats(+1, +1, Filters.EnchantedCreature)
        }

        staticAbility {
            ability = GrantWard(WardCost.Mana("{1}"))
        }

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/c/f/cff8ef48-2988-4d21-837e-01f1459e07c5.jpg?1782732082"
            artist = "Rovina Cai"
        }
    }

    /**
     * Cursed Role — Enchantment — Aura Role token (Wilds of Eldraine).
     * "Enchanted creature has base power and toughness 1/1."
     */
    val CursedRole = card("Cursed Role") {
        typeLine = "Enchantment — Aura Role"
        oracleText = "Enchant creature\nEnchanted creature has base power and toughness 1/1."

        auraTarget = Targets.Creature

        staticAbility {
            ability = SetBasePowerToughnessStatic(1, 1)
        }

        metadata {
            // "Wicked // Cursed" flip token: the single image shows Wicked upright; Cursed is the
            // bottom face and reads upside-down, so rotate 180° to display it correctly.
            imageUri = "https://cards.scryfall.io/normal/front/a/9/a9b7040e-cd24-42cc-b043-9af8c557da6a.jpg?1782732081"
            imageRotation = 180
            artist = "Rovina Cai"
        }
    }

    /**
     * Wicked Role — Enchantment — Aura Role token (Wilds of Eldraine).
     * "Enchanted creature gets +1/+1. When this Role is put into a graveyard, each opponent loses 1 life."
     */
    val WickedRole = card("Wicked Role") {
        typeLine = "Enchantment — Aura Role"
        oracleText = "Enchant creature\nEnchanted creature gets +1/+1.\n" +
            "When this Role is put into a graveyard, each opponent loses 1 life."

        auraTarget = Targets.Creature

        staticAbility {
            ability = ModifyStats(+1, +1, Filters.EnchantedCreature)
        }

        // The Role leaving the battlefield for the graveyard — destroyed, sacrificed, replaced by
        // another Role, or falling off as its creature leaves — drains each opponent for 1.
        triggeredAbility {
            trigger = Triggers.PutIntoGraveyardFromBattlefield
            effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
        }

        metadata {
            // "Wicked // Cursed" flip token: Wicked is the upright (front) face — no rotation.
            imageUri = "https://cards.scryfall.io/normal/front/a/9/a9b7040e-cd24-42cc-b043-9af8c557da6a.jpg?1782732081"
            artist = "Rovina Cai"
        }
    }

    /**
     * Young Hero Role — Enchantment — Aura Role token (Wilds of Eldraine).
     * "Enchanted creature has 'Whenever this creature attacks, if its toughness is 3 or less,
     *  put a +1/+1 counter on it.'"
     */
    val YoungHeroRole = card("Young Hero Role") {
        typeLine = "Enchantment — Aura Role"
        oracleText = "Enchant creature\nEnchanted creature has \"Whenever this creature attacks, " +
            "if its toughness is 3 or less, put a +1/+1 counter on it.\""

        auraTarget = Targets.Creature

        // The granted ability is modeled as an ATTACHED-bound trigger on the Role watching its
        // enchanted creature attack; the intervening-if re-checks the toughness at resolution (CR 603.4).
        triggeredAbility {
            trigger = Triggers.attacks(binding = TriggerBinding.ATTACHED)
            triggerCondition = Conditions.EntityMatches(
                EffectTarget.EnchantedCreature,
                GameObjectFilter.Creature.toughnessAtMost(3)
            )
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.EnchantedCreature)
        }

        metadata {
            // "Royal // Young Hero" flip token: Young Hero is the bottom face — rotate 180°.
            imageUri = "https://cards.scryfall.io/normal/front/c/f/cff8ef48-2988-4d21-837e-01f1459e07c5.jpg?1783914992"
            imageRotation = 180
            artist = "Rovina Cai"
        }
    }

    /**
     * Phyrexian — back face of the Incubator token.
     * Colorless 0/0 Phyrexian artifact creature.
     */
    val Phyrexian = card("Phyrexian") {
        typeLine = "Artifact Creature — Phyrexian"
        power = 0
        toughness = 0

        metadata {
            imageUri = "https://cards.scryfall.io/normal/back/c/c/cca1decc-90fd-4df8-997e-52f8789032f8.jpg?1682207112"
            artist = "Johann Bodin"
        }
    }

    /**
     * Incubator — front face of the Incubator token created by [Effects.Incubate].
     * Colorless artifact with "{2}: Transform this token." Transforms into [Phyrexian].
     *
     * Per CR 701.53b the token is a transforming double-faced permanent. The
     * `+1/+1` counters from "Incubate N" are placed by the [Effects.Incubate] composite,
     * not declared here.
     */
    val Incubator = doubleFacedPermanent(
        frontFace = card("Incubator") {
            typeLine = "Artifact — Incubator"

            activatedAbility {
                cost = Costs.Mana("{2}")
                effect = TransformEffect(EffectTarget.Self)
            }

            metadata {
                imageUri = "https://cards.scryfall.io/normal/front/c/c/cca1decc-90fd-4df8-997e-52f8789032f8.jpg?1682207112"
                artist = "Johann Bodin"
            }
        },
        backFace = Phyrexian
    )

    /**
     * Map token — an artifact with:
     * "{1}, {T}, Sacrifice this artifact: Target creature you control explores.
     *  Activate only as a sorcery."
     */
    val Map = card("Map") {
        typeLine = "Artifact — Map"

        activatedAbility {
            val creature = target("target creature you control", Targets.CreatureYouControl)
            cost = Costs.Composite(
                Costs.Mana("{1}"),
                Costs.Tap,
                Costs.SacrificeSelf
            )
            effect = Effects.Explore(creature)
            timing = TimingRule.SorcerySpeed
        }

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/6/4/64839118-09d2-4645-9d3c-f80755ac781f.jpg?1698873927"
            artist = "Francesca Baerald"
            collectorNumber = "17"
        }
    }

    /**
     * Drone token — a 1/1 colorless artifact creature token with:
     * Flying
     * "This token can block only creatures with flying."
     * Created by Desculpting Blast and other cards.
     */
    val Drone = card("Drone") {
        typeLine = "Artifact Creature — Drone"
        power = 1
        toughness = 1

        keywords(Keyword.FLYING)

        staticAbility {
            ability = CanOnlyBlockCreaturesWith(
                blockerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING)
            )
        }

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/3/f/3fcf8950-117a-4587-8522-79001dffa500.jpg?1752946472"
            artist = "Artur Nakhodkin"
        }
    }

    /**
     * Everywhere — a colorless land token created by Overlord of the Hauntwoods.
     * "This land is a Plains, Island, Swamp, Mountain, and Forest." Per the Scryfall
     * ruling it has the land types Plains/Island/Swamp/Mountain/Forest and the mana
     * ability of each basic land type (so {T} adds {W}, {U}, {B}, {R}, or {G}); it does
     * NOT have the basic supertype. Modeled with all five basic land subtypes on the type
     * line and a single tap-for-any-color mana ability — functionally identical to having
     * each basic land's mana ability.
     */
    val Everywhere = card("Everywhere") {
        typeLine = "Token Land — Plains Island Swamp Mountain Forest"

        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddAnyColorMana(1)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/6/b/6b76b7a4-f398-4bb4-833e-97ec0d4e581a.jpg?1726236906"
        }
    }

    /**
     * Munitions — a colorless artifact token created by Weapons Manufacturing with:
     * "When this token leaves the battlefield, it deals 2 damage to any target."
     */
    val Munitions = card("Munitions") {
        typeLine = "Artifact"

        triggeredAbility {
            trigger = Triggers.LeavesBattlefield
            target = Targets.Any
            effect = Effects.DealDamage(2, EffectTarget.ContextTarget(0))
            description = "When this token leaves the battlefield, it deals 2 damage to any target."
        }

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/a/1/a16f931a-9fa3-45b1-8d54-04b6b5bf7b71.jpg?1756281096"
        }
    }

    /**
     * Mutagen token — a colorless artifact (Teenage Mutant Ninja Turtles) with:
     * "{1}, {T}, Sacrifice this token: Put a +1/+1 counter on target creature.
     *  Activate only as a sorcery."
     *
     * Same shape as the [Map] token (mana + tap + sacrifice-self, sorcery-speed,
     * single creature target), but places a +1/+1 counter instead of an explore.
     */
    val Mutagen = card("Mutagen") {
        typeLine = "Artifact — Mutagen"

        activatedAbility {
            val creature = target("target creature", Targets.Creature)
            cost = Costs.Composite(
                Costs.Mana("{1}"),
                Costs.Tap,
                Costs.SacrificeSelf
            )
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature)
            timing = TimingRule.SorcerySpeed
        }

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/6/5/6559c423-449c-4e8e-8384-3ce78183e317.jpg?1760102885"
        }
    }

    /**
     * Frog token — a vanilla 1/1 green creature (Quina, Qu Gourmet).
     *
     * Its green color comes from a color indicator (CR 204), stored here via the
     * `colorIdentity` DSL setter → `colorIdentityOverride`. Tokens have no mana cost,
     * so the token-creation executors read the override for the printed color (a plain
     * `CardDefinition.colors` would be empty/colorless without a mana cost).
     */
    val Frog = card("Frog") {
        typeLine = "Creature — Frog"
        colorIdentity = "G"
        power = 1
        toughness = 1

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/e/3/e3c84944-23b8-40d7-9b25-c746b08b4dc4.jpg?1748704089"
            artist = "Daniel Correia"
        }
    }

    /**
     * All predefined token definitions.
     * Register these in the CardRegistry so token abilities are resolved.
     */
    val allTokens: List<CardDefinition> = listOf(
        Treasure,
        Meteorite,
        Food,
        Blood,
        Clue,
        Shard,
        Lander,
        JustOneGlass,
        Map,
        Sword,
        Cragflame,
        Mutavault,
        SorcererRole,
        MonsterRole,
        RoyalRole,
        CursedRole,
        WickedRole,
        YoungHeroRole,
        Incubator,
        Drone,
        Everywhere,
        Munitions,
        Mutagen,
        Frog
    )
}
